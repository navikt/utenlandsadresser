# Copilot instructions for utenlandsadresser

NAV service that shares foreign postal addresses (utenlandsadresser) with Skatteetaten through a subscription + feed API. Code, comments, domain names and docs are in Norwegian — keep that convention (e.g. `Løpenummer`, `Identitetsnummer`, `Abonnement`).

## Build, test, lint

Gradle multi-module (Kotlin, JVM toolchain 25). Modules: `app`, `felles`, `sporingslogg-cleanup`, `hent-utenlandsadresser`.

```sh
./gradlew app:test app:installDist          # what CI runs (per module, see .github/workflows/build.yaml)
./gradlew app:test --tests "no.nav.utenlandsadresser.app.FeedServiceTest"   # single spec
./gradlew app:run -Pdevelopment              # sets -Dio.ktor.development=true
./gradlew dependencyUpdates                  # ben-manes versions plugin
```

- Resolving `no.nav.pdl.libs:contract-pdl-avro` requires GitHub Packages credentials: env `READER_TOKEN` or Gradle property `maven.github.pdl.password`.
- Tests use Testcontainers (Postgres 18) and WireMock — Docker must be running.
- No lint task in Gradle; formatting follows ktlint (`kotlin.code.style=official`, IntelliJ ktlint plugin). Match existing style: trailing commas, multiline `when` branches with braces.
- Local run: `app/docker-compose.yaml` (app + Postgres); `export-env.sh` sets env vars. Kafka is not set up locally.

## Architecture

`app` is a Ktor (Netty) server. Wiring lives in `Application.module()` and runs in this order via functions in `setup/`: config → plugins → Flyway migration → repositories → clients → services → event consumers → background jobs → routes. Aggregates are plain data classes (`Repositories`, `Clients`, `Services`, `EventConsumers`, `Plugins`) — manual DI, no DI container. `module()` opens `context(appEnv, config, clock)` once; setup functions take the context they need, and what earlier setup steps produce is passed as normal parameters.

Data flow:
1. Consumer (Skatteetaten) calls `POST /api/v1/postadresse/abonnement/start` with Maskinporten token. `AbonnementService` creates the subscription and, if the person currently has a foreign address, immediately puts an event on the feed.
2. `KafkaPersonhendelseConsumer` (background coroutine in `launchBackgroundJobs`) reads PDL Leesah `Personhendelse` Avro events; only BOSTEDSADRESSE/KONTAKTADRESSE/ADRESSEBESKYTTELSE matter. `PostgresFeedEventCreator` writes a feed event per active subscription.
3. Consumer reads the feed with a `løpenummer` (sequence number, per organisasjonsnummer). `FeedService.readNext` fetches the *current* address from Registeroppslag (Team Dokumenthåndtering's API — we reuse their postadresse selection logic instead of calling PDL directly), writes a **sporingslogg** entry for every address handed out, and returns it. Feed events store only identitetsnummer + hendelsestype, never the address.
4. Address protection (adressebeskyttelse): graded addresses are never shared; an `Adressebeskyttelse` event is exposed as `SLETTET_ADRESSE` and the consumer is expected to delete the address.
5. Utenlandsk id is a separate subscription under `/api/v1/utenlandskid` with its own tables (`utenlandsk_id_abonnement`, `utenlandsk_id_feed`) and its own Maskinporten scope. `UtenlandskIdAbonnementService` looks up PDL (`PdlGraphQLClient`) before opening the DB transaction and puts an event on the feed if the person has a foreign ID. There is no Kafka event for utenlandsk id yet. `UtenlandskIdFeedService.readNext` looks up PDL again, logs sporingslogg for non-empty results and returns the current IDs (possibly an empty list). Address protection does not apply to utenlandsk id. All of `/api/v1/utenlandskid` sits behind the Unleash toggle `utenlandsadresser.utenlandsk-id` (`Toggle.UTENLANDSK_ID`): the route-scoped `FeatureToggleGate` plugin is installed outside `authenticate { }` and responds 404 when the toggle is off, checked per request. `FeatureToggles` is a port in `application/port/outbound/`; `UnleashFeatureToggles` is used in dev/prod (token from the `ApiToken` resource in `app/.nais/unleash-apitoken.yaml`), `StaticFeatureToggles.allEnabled()` locally and in tests. Remove the toggle when the feature is live in prod.
6. Stopped subscriptions: feed events stay in the feed after a subscription is stopped. Both `readNext` check `finnesAbonnement(abonnementId, organisasjonsnummer)` and return the event without data (null address or empty list), with no lookup and no sporingslogg. `SLETTET_ADRESSE` is always delivered. `utenlandsadresser_feed_utlevert_total{feed}` counts delivered data, and `utenlandsadresser_feed_stoppet_abonnement_total{feed}` counts reads for stopped subscriptions. The `feed` tag is `postadresse` or `utenlandskid`.

Løpenummer in both `feed` and `utenlandsk_id_feed` is assigned by `nesteLøpenummer` (`persistence/postgres/NesteLøpenummer.kt`): `max + 1` under a per-table-and-organisasjonsnummer `pg_advisory_xact_lock`, with PK `(organisasjonsnummer, løpenummer)` as a safety net. Call it inside the transaction that inserts the row.

Package layout under `no.nav.utenlandsadresser` (ports and adapters; `ArchitectureTest` enforces the dependency direction):
- `domain/` — value classes (`@JvmInline value class`) and sealed hierarchies. Depends on nothing else in the app.
- `application/service/` — services (use cases). Depend only on `domain` and `application/port/`.
- `application/port/outbound/` — interfaces the core needs (`AbonnementRepository`, `FeedRepository`, `SporingsloggRepository`, `RegisteroppslagClient`, `HentUtenlandskId`, `Metrikker`, `FeatureToggles`, …) with their error types.
- `adapter/inbound/web/` — Ktor routes, `json/` DTOs, `*RouteExamples.kt` for OpenAPI, and `plugin/` (Ktor plugins). Maskinporten auth validates the `consumer` claim's orgnr against `maskinporten.consumers` config and stores it in `call.attributes[OrganisasjonsnummerKey]`. There are two auth configurations: `POSTADRESSE_MASKINPORTEN_AUTH` requires `maskinporten.postadresseScope` and `UTENLANDSK_ID_MASKINPORTEN_AUTH` requires `maskinporten.utenlandskIdScope`.
- `adapter/inbound/kafka/` — `KafkaPersonhendelseConsumer`.
- `adapter/outbound/` — `persistence/postgres/`, `registeroppslag/`, `pdl/`, `maskinporten/`, `http/`, `metrics/`, `unleash/`.
- `adapter/health/` — `HealthCheck`, shared by web and kafka.
- `config/`, `setup/` and the aggregates in the root package are the composition root.

Routes under `/internal` are hidden from OpenAPI; `/internal/dev` routes are only registered for `LOCAL`/`DEV_GCP`. `/internal/sporingslogg` (DELETE `?olderThan=`) is called monthly by the `sporingslogg-cleanup` naisjob to delete sporingslogg older than 10 years.

Shared code lives in `felles` (`no.nav.utenlandsadresser.felles`): `AppEnv`, `configureLogging`, `createHttpClient`/`createAuthHttpClient` with `BearerAuthPlugin`, `OAuthConfig`, `Scope`, `BearerToken`, `years`, and `SporingsloggJson` (the HTTP contract for `POST /internal/sporingslogg`, with `String` fields). `felles` holds no domain types. All other modules depend on `project(":felles")`; no module depends on `app`:
- `sporingslogg-cleanup` — one-shot job calling the app's cleanup endpoint.
- `hent-utenlandsadresser` — POC fetching addresses and pushing to PDL mottak.

## Conventions

- **Errors via Arrow**: services return `Either<SealedError, T>` built with `either { ... raise(...) }`; routes map each error case exhaustively with `getOrElse { when (it) { ... } }`. Don't throw for expected failures.
- **Dependencies**: the constructor takes what the logic needs, including `kotlin.time.Clock` (never call `Clock.System` outside `module()`; tests use `FastClock`). Tracking that doesn't change behaviour goes in a context parameter opened at the edge: `readNext` takes `context(metrikker: Metrikker)`, and the route calls `context(metrikker) { feedService.readNext(...) }`. `Metrikker` is a port in `application/port/outbound/`; `MicrometerMetrikker` is the only class that knows Micrometer. Tests use `TestMetrikker` or `NoopMetrikker`; mock with `coEvery { context(NoopMetrikker) { feedService.readNext(any(), any()) } }`.
- **Logging**: each class has `private val logger = LoggerFactory.getLogger(X::class.java)` with the explicit class. Top-level route/plugin functions use a private top-level logger with a fixed name. Never pass a logger in.
- **Persistence**: Exposed **R2DBC** (non-blocking) — `org.jetbrains.exposed.v1.*` imports, all DB access inside `suspendTransaction(db = database, readOnly = ...)`. Repository classes extend `Table("name")` directly and define columns as private properties. Domain ↔ DB mapping via `*Postgres`/`*Dto` types with `toDomain()`/`fromDomain()`.
- **Migrations**: Flyway, `app/src/main/resources/db/migration/V<n>__Description.sql`. Never edit existing migrations; add a new version. Tests run migrations from that filesystem path.
- **JSON DTOs** live in `json/` packages named `*Json`, with `fromDomain`/`toDomain` companions; domain types stay separate from wire formats.
- **Config**: Hoplite HOCON. `application.conf` is the prod baseline; `application-local.conf` / `application-dev-gcp.conf` are layered on top based on `APP_ENV` (`local` | `dev-gcp` | `prod-gcp`, defaults to local). Add new config to the relevant `*Config` data class in `config/` and all conf files.
- **Avro**: `org.apache.avro.SERIALIZABLE_PACKAGES=no.nav.person.pdl.leesah` must be set (Gradle sets it for run/test; Dockerfile sets it in `CMD`).
- **Tests**: Kotest (mostly `WordSpec`) + MockK. Reusable extensions in `app/src/test/.../kotest/extension/`: `setupDatabase()` (shared Postgres container, `flyway.clean()+migrate()` before each test), `setupWiremockServer()`, `specWideTestApplication { }` for Ktor route tests. Kotest classpath scanning is disabled; project config is set via `kotest.properties` (`kotest.framework.config.fqn`).
- **Deploy**: NAIS on GCP. Each module has `.nais/nais.yaml` + `.nais/vars/{dev,prod}.yaml` and its own workflow; images are only pushed/deployed from `main`.
