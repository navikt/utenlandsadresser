package no.nav.utenlandsadresser

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.clients.http.configureAuthHttpClient
import no.nav.utenlandsadresser.clients.http.configureHttpClient
import no.nav.utenlandsadresser.clients.http.maskinporten.MaskinportenHttpClient
import no.nav.utenlandsadresser.clients.http.plugins.configureBehandlingskatalogBehandlingsnummerHeader
import no.nav.utenlandsadresser.clients.http.regoppslag.RegisteroppslagHttpClient
import no.nav.utenlandsadresser.config.*
import no.nav.utenlandsadresser.database.exposed.AbonnementExposedRepository
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.plugins.configureBasicAuthDev
import no.nav.utenlandsadresser.plugins.configureMetrics
import no.nav.utenlandsadresser.plugins.configureSerialization
import no.nav.utenlandsadresser.routes.configureDevRoutes
import no.nav.utenlandsadresser.routes.configureLivenessRoute
import no.nav.utenlandsadresser.routes.configurePostadresseRoutes
import no.nav.utenlandsadresser.routes.configureReadinessRoute
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

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = System.getenv("NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_URL")
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 5
    }

    val dataSource = HikariDataSource(hikariConfig)
    val database = Database.connect(dataSource)
    val abonnementRepository = AbonnementExposedRepository(database)

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
        .configureBehandlingskatalogBehandlingsnummerHeader(
            behandlingsnummer
        )
    val regOppslagClient = RegisteroppslagHttpClient(
        regoppslagAuthHttpClient,
        Url(applicationConfig.getString("regoppslag.baseUrl"))
    )

    val maskinportenConfig = MaskinportenConfig.getFromEnv()
    val httpClient = configureHttpClient()
    val maskinportenClient = MaskinportenHttpClient(
        maskinportenConfig,
        httpClient,
    )

    val abonnementService = AbonnementService(abonnementRepository)

    // Configure basic auth for dev API
    configureBasicAuthDev(getDevApiBasicAuthConfig(logger))
    configureMetrics()
    configureSerialization()

    routing {
        // TODO: Move to application config
        configurePostadresseRoutes(Scope("nav:utenlandsadresser:postadresse.read"), abonnementService)
        configureLivenessRoute()
        configureReadinessRoute()
        when (ktorEnv) {
            KtorEnv.LOCAL,
            KtorEnv.DEV_GCP -> configureDevRoutes(regOppslagClient, maskinportenClient)

            KtorEnv.PROD_GCP -> {}
        }
    }
}
