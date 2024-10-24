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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
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
import no.nav.utenlandsadresser.infrastructure.kafka.LivshendelserKafkaConsumer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.AbonnementPostgresRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.FeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.FeedPostgresRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.SporingsloggPostgresRepository
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

fun main() {
    configureLogging(AppEnv.getFromEnvVariable("APP_ENV"))
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

@OptIn(ExperimentalHoplite::class)
fun Application.module() {
    val logger = LoggerFactory.getLogger(this::class.java)
    val appEnv = AppEnv.getFromEnvVariable("APP_ENV")
    val resourceFiles =
        listOf(
            when (appEnv) {
                AppEnv.LOCAL -> "/application-local.conf"
                AppEnv.DEV_GCP -> "/application-dev-gcp.conf"
                AppEnv.PROD_GCP -> "/application-prod-gcp.conf"
            },
            "/application.conf",
        )
    val config: UtenlandsadresserConfig =
        ConfigLoaderBuilder
            .default()
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow(resourceFiles)

    logger.info("Starting application in $appEnv")
    val utenlandsadresserDatabaseConfig =
        when (appEnv) {
            AppEnv.LOCAL -> startLocalPostgresContainer()
            AppEnv.DEV_GCP,
            AppEnv.PROD_GCP,
            -> config.utenlandsadresserDatabase
        }

    val hikariConfig = createHikariConfig(utenlandsadresserDatabaseConfig)
    val dataSource = HikariDataSource(hikariConfig)

    // Kjør migrering av databasen før det opprettes tilkoblinger til databasen
    flywayMigration(dataSource)

    val database = Database.connect(dataSource)
    val abonnementRepository = AbonnementPostgresRepository(database)
    val feedRepository = FeedPostgresRepository(database)
    val abonnementInitializer = PostgresAbonnementInitializer(abonnementRepository, feedRepository)
    val sporingslogg = SporingsloggPostgresRepository(database)

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

    val abonnementService = AbonnementService(abonnementRepository, regOppslagClient, abonnementInitializer)
    val feedService =
        FeedService(feedRepository, regOppslagClient, sporingslogg, LoggerFactory.getLogger(FeedService::class.java))

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

    val feedEventCreator =
        FeedEventCreator(
            feedRepository,
            abonnementRepository,
        )
    val livshendelserConsumer =
        LivshendelserKafkaConsumer(
            kafkaConsumer,
            feedEventCreator,
            LoggerFactory.getLogger(LivshendelserKafkaConsumer::class.java),
        )

    launch(Dispatchers.IO) {
        with(livshendelserConsumer) {
            use {
                while (isActive) {
                    consumeLivshendelser(config.kafka.topic)
                }
            }
        }
    }

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
        jwkProvider = JwkProviderBuilder(URI(config.maskinporten.jwksUri).toURL()).build(),
        jwtValidationBlock = validateOrganisasjonsnummer(config.maskinporten.consumers),
    )
    configureSwagger()

    routing {
        configurePostadresseRoutes(
            abonnementService,
            feedService,
        )
        route("/internal") {
            configureLivenessRoute(
                logger = LoggerFactory.getLogger("LivenessRoute"),
                healthChecks = listOf(livshendelserConsumer),
            )
            configureReadinessRoute()
            configureSporingsloggCleanupRoute(sporingslogg)
            when (appEnv) {
                AppEnv.LOCAL,
                AppEnv.DEV_GCP,
                -> configureDevRoutes(regOppslagClient, maskinportenClient)

                AppEnv.PROD_GCP -> {}
            }
        }
    }
}
