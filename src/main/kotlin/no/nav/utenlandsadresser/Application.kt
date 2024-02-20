package no.nav.utenlandsadresser

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.configureLogging
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
import no.nav.utenlandsadresser.plugin.*
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
    val config = ConfigLoaderBuilder.default()
        .apply {
            when (appEnv) {
                AppEnv.LOCAL -> addResourceSource("/application-local.conf")
                AppEnv.DEV_GCP -> addResourceSource("/application-dev-gcp.conf")
                AppEnv.PROD_GCP -> addResourceSource("/application-prod-gcp.conf")
            }
        }
        .build()
        .loadConfigOrThrow<UtenlandsadresserConfig>()

    logger.info("Starting application in $appEnv")
    logger.info("Config: $config")

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.utenlandsadresserDatabase.jdbcUrl
        username = config.utenlandsadresserDatabase.username
        password = config.utenlandsadresserDatabase.password?.value
        driverClassName = config.utenlandsadresserDatabase.driverClassName
        maximumPoolSize = 10
        minimumIdle = 5
    }

    val dataSource = HikariDataSource(hikariConfig)
    configureFlyway(dataSource)
    val database = Database.connect(dataSource)
    val abonnementRepository = AbonnementExposedRepository(database)
    val feedRepository = FeedExposedRepository(database)
    val initAbonnement = ExposedInitAbonnement(abonnementRepository, feedRepository)

    val behandlingsnummer = BehandlingskatalogBehandlingsnummer(config.behandlingskatalogBehandlingsnummer.value)
    val regoppslagAuthHttpClient = configureAuthHttpClient(config.oAuth)

    val regOppslagClient = RegisteroppslagHttpClient(
        regoppslagAuthHttpClient,
        Url(config.registeroppslag.baseUrl),
        behandlingsnummer,
    )

    val httpClient = configureHttpClient()
    val maskinportenClient = MaskinportenHttpClient(
        config.maskinporten,
        httpClient,
    )


    val abonnementService = AbonnementService(abonnementRepository, regOppslagClient, initAbonnement)
    val feedService = FeedService(feedRepository, regOppslagClient, LoggerFactory.getLogger(FeedService::class.java))

    // Configure basic auth for dev API
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
