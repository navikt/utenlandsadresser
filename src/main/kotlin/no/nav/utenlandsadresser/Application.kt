package no.nav.utenlandsadresser

import com.sksamuel.hoplite.ConfigLoader
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.config.createHikariConfig
import no.nav.utenlandsadresser.config.kafkConsumerConfig
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
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
import no.nav.utenlandsadresser.infrastructure.route.configureDevRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureLivenessRoute
import no.nav.utenlandsadresser.infrastructure.route.configurePostadresseRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureReadinessRoute
import no.nav.utenlandsadresser.plugin.*
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

fun main() {
    configureLogging(AppEnv.getFromEnvVariable("APP_ENV"))
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger(this::class.java)
    val appEnv = AppEnv.getFromEnvVariable("APP_ENV")
    val resourceFiles = listOf(
        when (appEnv) {
            AppEnv.LOCAL -> "/application-local.conf"
            AppEnv.DEV_GCP -> "/application-dev-gcp.conf"
            AppEnv.PROD_GCP -> "/application-prod-gcp.conf"
        },
        "/application.conf"
    )
    val config: UtenlandsadresserConfig = ConfigLoader().loadConfigOrThrow(resourceFiles)

    logger.info("Starting application in $appEnv")
    when (appEnv) {
        AppEnv.LOCAL,
        AppEnv.DEV_GCP -> logger.info("Config: $config")

        AppEnv.PROD_GCP -> {}
    }

    val hikariConfig = createHikariConfig(config.utenlandsadresserDatabase)
    val dataSource = HikariDataSource(hikariConfig)

    // Kjør migrering av databasen før det opprettes tilkoblinger til databasen
    flywayMigration(dataSource)

    val database = Database.connect(dataSource)
    val abonnementRepository = AbonnementPostgresRepository(database)
    val feedRepository = FeedPostgresRepository(database)
    val abonnementInitializer = PostgresAbonnementInitializer(abonnementRepository, feedRepository)

    val regoppslagAuthHttpClient = configureAuthHttpClient(
        config.oAuth,
        listOf(Scope(config.registeroppslag.scope)),
    )

    val regOppslagClient = RegisteroppslagHttpClient(
        regoppslagAuthHttpClient,
        Url(config.registeroppslag.baseUrl),
        BehandlingskatalogBehandlingsnummer(config.behandlingskatalogBehandlingsnummer.value),
    )

    val httpClient = configureHttpClient()
    val maskinportenClient = MaskinportenHttpClient(
        config.maskinporten,
        httpClient,
    )

    val abonnementService = AbonnementService(abonnementRepository, regOppslagClient, abonnementInitializer)
    val feedService = FeedService(feedRepository, regOppslagClient, LoggerFactory.getLogger(FeedService::class.java))

    val kafkaConsumer: KafkaConsumer<String, GenericRecord> = KafkaConsumer(
        kafkConsumerConfig(config.kafka),
    )
    val feedEventCreator = FeedEventCreator(
        feedRepository,
        abonnementRepository,
        LoggerFactory.getLogger(FeedEventCreator::class.java)
    )
    val livshendelserConsumer =
        LivshendelserKafkaConsumer(
            kafkaConsumer,
            feedEventCreator,
            LoggerFactory.getLogger(LivshendelserKafkaConsumer::class.java),
        )

    when (appEnv) {
        AppEnv.DEV_GCP,
        AppEnv.PROD_GCP -> {
            launch(Dispatchers.IO) {
                with(livshendelserConsumer) {
                    consumeLivshendelser(config.kafka.topic)
                }
            }
        }

        AppEnv.LOCAL -> {}
    }

    configureBasicAuthDev(config.basicAuth)
    configureMetrics()
    configureSerialization()

    configureSwagger()

    routing {
        configurePostadresseRoutes(Scope(config.maskinporten.scopes), abonnementService, feedService)
        configureLivenessRoute()
        configureReadinessRoute()
        when (appEnv) {
            AppEnv.LOCAL,
            AppEnv.DEV_GCP -> configureDevRoutes(regOppslagClient, maskinportenClient)

            AppEnv.PROD_GCP -> {}
        }
    }
}
