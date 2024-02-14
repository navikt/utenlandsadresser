package no.nav.utenlandsadresser

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.config.*
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.client.http.configureAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.configureHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.maskinporten.MaskinportenHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.RegisteroppslagHttpClient
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.AbonnementExposedRepository
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.ExposedInitAbonnement
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.FeedExposedRepository
import no.nav.utenlandsadresser.infrastructure.route.configureDevRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureLivenessRoute
import no.nav.utenlandsadresser.infrastructure.route.configurePostadresseRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureReadinessRoute
import no.nav.utenlandsadresser.plugin.configureBasicAuthDev
import no.nav.utenlandsadresser.plugin.configureFlyway
import no.nav.utenlandsadresser.plugin.configureMetrics
import no.nav.utenlandsadresser.plugin.configureSerialization
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

fun main() {
    configureLogging(KtorEnv.getFromEnvVariable("KTOR_ENV"))
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger(this::class.java)
    val ktorEnv = KtorEnv.getFromEnvVariable("KTOR_ENV")

    logger.info("Starting application in $ktorEnv")

    val databaseHost = System.getenv("NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_HOST")
    val databasePort = System.getenv("NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_PORT")
    val databaseName = System.getenv("NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_DATABASE")
    val databaseUsername = System.getenv("NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_USERNAME")
    val databasePassword = System.getenv("NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_PASSWORD")
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$databaseHost:$databasePort/$databaseName"
        username = databaseUsername
        password = databasePassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 5
    }

    val dataSource = HikariDataSource(hikariConfig)
    configureFlyway(dataSource)
    val database = Database.connect(dataSource)
    val abonnementRepository = AbonnementExposedRepository(database)
    val feedRepository = FeedExposedRepository(database)
    val initAbonnement = ExposedInitAbonnement(abonnementRepository, feedRepository)

    val applicationConfig = getApplicationConfig(ktorEnv)

    val regoppslagOAuthConfig = getOAuthConfigFromEnv(
        applicationConfig.getString("regoppslag.scope"),
        logger
    )
    val behandlingsnummer = BehandlingskatalogBehandlingsnummer(
        System.getenv("BEHANDLINGSKATALOG_BEHANDLINGSNUMMER")
            ?: throw IllegalStateException("Environment variable BEHANDLINGSKATALOG_BEHANDLINGSNUMMER not set")
    )
    val regoppslagAuthHttpClient = configureAuthHttpClient(regoppslagOAuthConfig)

    val regOppslagClient = RegisteroppslagHttpClient(
        regoppslagAuthHttpClient,
        Url(applicationConfig.getString("regoppslag.baseUrl")),
        behandlingsnummer,
    )

    val maskinportenConfig = MaskinportenConfig.getFromEnv()
    val httpClient = configureHttpClient()
    val maskinportenClient = MaskinportenHttpClient(
        maskinportenConfig,
        httpClient,
    )


    val abonnementService = AbonnementService(abonnementRepository, regOppslagClient, initAbonnement)
    val feedService = FeedService(feedRepository, regOppslagClient)

    // Configure basic auth for dev API
    configureBasicAuthDev(getDevApiBasicAuthConfig(logger))
    configureMetrics()
    configureSerialization()

    routing {
        // TODO: Move to application config
        configurePostadresseRoutes(Scope("nav:utenlandsadresser:postadresse.read"), abonnementService, feedService)
        configureLivenessRoute()
        configureReadinessRoute()
        when (ktorEnv) {
            KtorEnv.LOCAL,
            KtorEnv.DEV_GCP -> configureDevRoutes(regOppslagClient, maskinportenClient)

            KtorEnv.PROD_GCP -> {}
        }
    }
}
