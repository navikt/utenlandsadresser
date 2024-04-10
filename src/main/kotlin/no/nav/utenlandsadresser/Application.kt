package no.nav.utenlandsadresser

import com.auth0.jwk.JwkProviderBuilder
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.Masked
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.config.*
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.client.http.configureAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.configureHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.maskinporten.MaskinportenHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.RegisteroppslagHttpClient
import no.nav.utenlandsadresser.infrastructure.kafka.LivshendelserKafkaConsumer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.*
import no.nav.utenlandsadresser.infrastructure.route.configureDevRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureLivenessRoute
import no.nav.utenlandsadresser.infrastructure.route.configurePostadresseRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureReadinessRoute
import no.nav.utenlandsadresser.plugin.configureMetrics
import no.nav.utenlandsadresser.plugin.configureSerialization
import no.nav.utenlandsadresser.plugin.configureSwagger
import no.nav.utenlandsadresser.plugin.flywayMigration
import no.nav.utenlandsadresser.plugin.maskinporten.configureMaskinportenAuthentication
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.jetbrains.exposed.sql.Database
import org.postgresql.Driver
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.net.URL

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
    val utenlandsadresserDatabaseConfig = when (appEnv) {
        AppEnv.LOCAL -> {
            PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:15-alpine")).apply {
                withDatabaseName("utenlandsadresser")
                withUsername("utenlandsadresser")
                withPassword("utenlandsadresser")
                start()
            }.let {
                UtenlandsadresserDatabaseConfig(
                    username = it.username,
                    password = Masked(it.password),
                    driverClassName = Driver::class.qualifiedName!!,
                    jdbcUrl = it.jdbcUrl,
                )
            }
        }

        AppEnv.DEV_GCP,
        AppEnv.PROD_GCP -> config.utenlandsadresserDatabase
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
    val feedService =
        FeedService(feedRepository, regOppslagClient, sporingslogg, LoggerFactory.getLogger(FeedService::class.java))

    val kafkaConsumer: Consumer<String, GenericRecord> = when (appEnv) {
        AppEnv.LOCAL -> MockConsumer(OffsetResetStrategy.LATEST)
        AppEnv.DEV_GCP,
        AppEnv.PROD_GCP -> KafkaConsumer(
            kafkConsumerConfig(config.kafka),
        )
    }
    kafkaConsumer.subscribe(listOf(config.kafka.topic))

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

    launch(Dispatchers.IO) {
        with(livshendelserConsumer) {
            this.use {
                while (isActive) {
                    consumeLivshendelser(config.kafka.topic)
                }
            }
        }
    }

    configureMetrics()
    configureSerialization()
    configureMaskinportenAuthentication(
        issuer = Issuer(config.maskinporten.issuer),
        expectedScopes = config.maskinporten.scopes.split(" ").map(::Scope).toSet(),
        jwkProvider = JwkProviderBuilder(URL(config.maskinporten.jwksUri)).build()
    )
    configureSwagger()

    routing {
        configurePostadresseRoutes(
            Scope(config.maskinporten.scopes),
            config.maskinporten.consumers.map(::Organisasjonsnummer).toSet(),
            abonnementService,
            feedService,
        )
        configureLivenessRoute()
        configureReadinessRoute()
        when (appEnv) {
            AppEnv.LOCAL,
            AppEnv.DEV_GCP -> configureDevRoutes(regOppslagClient, maskinportenClient)

            AppEnv.PROD_GCP -> {}
        }
    }
}
