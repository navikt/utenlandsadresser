package no.nav.utenlandsadresser

import arrow.core.toNonEmptySetOrNull
import com.auth0.jwk.JwkProviderBuilder
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.Url
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.config.UtenlandsadresserConfiguration
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.config.createHikariConfig
import no.nav.utenlandsadresser.config.kafkConsumerConfig
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.client.http.configureAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.configureHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.maskinporten.MaskinportenHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.RegisteroppslagHttpClient
import no.nav.utenlandsadresser.infrastructure.kafka.KafkaLivshendelserConsumer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresSporingsloggRepository
import no.nav.utenlandsadresser.infrastructure.route.configureDevRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureLivenessRoute
import no.nav.utenlandsadresser.infrastructure.route.configurePostadresseRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureReadinessRoute
import no.nav.utenlandsadresser.infrastructure.route.configureSporingsloggCleanupRoute
import no.nav.utenlandsadresser.plugin.configureCallLogging
import no.nav.utenlandsadresser.plugin.configureMetrics
import no.nav.utenlandsadresser.plugin.configureSerialization
import no.nav.utenlandsadresser.plugin.configureSwagger
import no.nav.utenlandsadresser.plugin.flywayMigration
import no.nav.utenlandsadresser.plugin.maskinporten.configureMaskinportenAuthentication
import no.nav.utenlandsadresser.plugin.maskinporten.validateOrganisasjonsnummer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.net.URI
import javax.sql.DataSource

fun main() {
    configureLogging(AppEnv.getFromEnvVariable("APP_ENV"))
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

private fun Application.module() {
    val logger = LoggerFactory.getLogger(this::class.java)
    val appEnv = AppEnv.getFromEnvVariable("APP_ENV")
    logger.info("Starting application in $appEnv")

    val config: UtenlandsadresserConfiguration = loadConfiguration(appEnv)

    val dataSource: DataSource = configureDataSource(appEnv, config)

    // Kjør migrering av databasen før det opprettes tilkoblinger til databasen
    flywayMigration(dataSource)

    val repositories = configureRepositories(dataSource)
    val clients = configureClients(config)
    val services = configureServices(repositories, clients)
    val eventConsumers = configureEventConsumers(appEnv, config, repositories)

    launchBackgroundJobs(eventConsumers, config)
    configureApplicationPlugins(config)
    configureRoutes(services, eventConsumers, repositories, appEnv, clients)
}

private fun Application.configureRoutes(
    services: Services,
    eventConsumers: EventConsumers,
    repositories: Repositories,
    appEnv: AppEnv,
    clients: Clients,
) {
    routing {
        configurePostadresseRoutes(
            services.abonnementService,
            services.feedService,
        )
        route("/internal") {
            configureLivenessRoute(
                logger = LoggerFactory.getLogger("LivenessRoute"),
                healthChecks = listOf(eventConsumers.livshendelserConsumer),
            )
            configureReadinessRoute()
            configureSporingsloggCleanupRoute(repositories.sporingsloggRepository)
            when (appEnv) {
                AppEnv.LOCAL,
                AppEnv.DEV_GCP,
                -> configureDevRoutes(clients.regOppslagClient, clients.maskinportenClient)

                AppEnv.PROD_GCP -> {}
            }
        }
    }
}

private fun CoroutineScope.launchBackgroundJobs(
    eventConsumers: EventConsumers,
    config: UtenlandsadresserConfiguration,
) {
    launch(Dispatchers.IO) {
        with(eventConsumers.livshendelserConsumer) {
            use {
                while (isActive) {
                    consumeLivshendelser(config.kafka.topic)
                }
            }
        }
    }
}

private fun Application.configureApplicationPlugins(config: UtenlandsadresserConfiguration) {
    configureMetrics()
    configureSerialization()
    configureCallLogging()
    configureMaskinportenAuthentication(
        configurationName = "postadresse-abonnement-maskinporten",
        issuer = Issuer(config.maskinporten.issuer),
        requiredScopes =
            config.maskinporten.scopes
                .split(" ")
                .map(::Scope)
                .toNonEmptySetOrNull() ?: throw IllegalArgumentException("Missing required scopes"),
        jwkProvider = JwkProviderBuilder(URI.create(config.maskinporten.jwksUri).toURL()).build(),
        jwtValidationBlock = validateOrganisasjonsnummer(config.maskinporten.consumers),
    )
    configureSwagger()
}

private fun configureEventConsumers(
    appEnv: AppEnv,
    config: UtenlandsadresserConfiguration,
    repositories: Repositories,
): EventConsumers {
    val kafkaConsumer: Consumer<String, GenericRecord> =
        when (appEnv) {
            AppEnv.LOCAL -> MockConsumer(OffsetResetStrategy.LATEST)
            AppEnv.DEV_GCP,
            AppEnv.PROD_GCP,
            ->
                KafkaConsumer(
                    kafkConsumerConfig(config.kafka),
                )
        }

    kafkaConsumer.subscribe(listOf(config.kafka.topic))

    return EventConsumers(
        livshendelserConsumer =
            KafkaLivshendelserConsumer(
                kafkaConsumer,
                repositories.feedEventCreator,
                LoggerFactory.getLogger(KafkaLivshendelserConsumer::class.java),
            ),
    )
}

private fun configureServices(
    repositories: Repositories,
    clients: Clients,
): Services {
    val abonnementService =
        AbonnementService(
            repositories.abonnementRepository,
            clients.regOppslagClient,
            repositories.abonnementInitializer,
        )
    val feedService =
        FeedService(
            repositories.feedRepository,
            clients.regOppslagClient,
            repositories.sporingsloggRepository,
            LoggerFactory.getLogger(FeedService::class.java),
        )

    return Services(
        abonnementService = abonnementService,
        feedService = feedService,
    )
}

private fun configureClients(config: UtenlandsadresserConfiguration): Clients {
    val regoppslagAuthHttpClient =
        configureAuthHttpClient(
            config.oAuth,
            listOf(Scope(config.registeroppslag.scope)),
        )

    val regOppslagClient =
        RegisteroppslagHttpClient(
            regoppslagAuthHttpClient,
            Url(config.registeroppslag.baseUrl),
            BehandlingskatalogBehandlingsnummer(config.behandlingskatalogBehandlingsnummer.value),
        )

    val httpClient = configureHttpClient()
    val maskinportenClient =
        MaskinportenHttpClient(
            config.maskinporten,
            httpClient,
        )

    return Clients(
        regOppslagClient = regOppslagClient,
        maskinportenClient = maskinportenClient,
    )
}

private fun configureRepositories(dataSource: DataSource): Repositories {
    val database = Database.connect(dataSource)
    val abonnementRepository = PostgresAbonnementRepository(database)
    val feedRepository = PostgresFeedRepository(database)
    val abonnementInitializer = PostgresAbonnementInitializer(abonnementRepository, feedRepository)
    val sporingslogg = PostgresSporingsloggRepository(database)
    val feedEventCreator = PostgresFeedEventCreator(feedRepository, abonnementRepository)

    return Repositories(
        abonnementRepository = abonnementRepository,
        abonnementInitializer = abonnementInitializer,
        feedRepository = feedRepository,
        sporingsloggRepository = sporingslogg,
        feedEventCreator = feedEventCreator,
    )
}

private fun configureDataSource(
    appEnv: AppEnv,
    config: UtenlandsadresserConfiguration,
): HikariDataSource {
    val utenlandsadresserDatabaseConfig =
        when (appEnv) {
            AppEnv.LOCAL -> startLocalPostgresContainer()
            AppEnv.DEV_GCP,
            AppEnv.PROD_GCP,
            -> config.utenlandsadresserDatabase
        }

    val hikariConfig = createHikariConfig(utenlandsadresserDatabaseConfig)
    return HikariDataSource(hikariConfig)
}

@OptIn(ExperimentalHoplite::class)
private fun loadConfiguration(appEnv: AppEnv): UtenlandsadresserConfiguration {
    val resourceFiles =
        listOf(
            when (appEnv) {
                AppEnv.LOCAL -> "/application-local.conf"
                AppEnv.DEV_GCP -> "/application-dev-gcp.conf"
                AppEnv.PROD_GCP -> "/application-prod-gcp.conf"
            },
            "/application.conf",
        )
    return ConfigLoaderBuilder
        .default()
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<UtenlandsadresserConfiguration>(resourceFiles)
}
