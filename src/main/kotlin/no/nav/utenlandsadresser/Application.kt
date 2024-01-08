package no.nav.utenlandsadresser

import arrow.core.getOrElse
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.clients.http.configureAuthHttpClient
import no.nav.utenlandsadresser.clients.http.pdl.PdlHttpClient
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.clients.http.plugins.configureBehandlingskatalogBehandlingsnummerHeader
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.config.getApplicationConfig
import no.nav.utenlandsadresser.config.getBasicAuthConfigFromEnv
import no.nav.utenlandsadresser.config.getOAuthConfigFromEnv
import no.nav.utenlandsadresser.domain.BaseUrl
import no.nav.utenlandsadresser.plugins.configureBasicAuthDev
import no.nav.utenlandsadresser.plugins.configureMetrics
import no.nav.utenlandsadresser.plugins.configureSerialization
import no.nav.utenlandsadresser.routes.configureDevRoutes
import no.nav.utenlandsadresser.routes.configureLivenessRoute
import no.nav.utenlandsadresser.routes.configureReadinessRoute
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

    val applicationConfig = getApplicationConfig(ktorEnv)

    // Configure basic auth for dev API
    val basicAuthConfig = getBasicAuthConfigFromEnv(logger)
    configureBasicAuthDev(basicAuthConfig)

    val oAuthConfig = getOAuthConfigFromEnv(logger, applicationConfig)
    val behandlingsnummer = BehandlingskatalogBehandlingsnummer(
        System.getenv("BEHANDLINGSKATALOG_BEHANDLINGSNUMMER")
            ?: throw RuntimeException("Environment variable BEHANDLINGSKATALOG_BEHANDLINGSNUMMER not set")
    )
    val authHttpClient = configureAuthHttpClient(oAuthConfig)
        .configureBehandlingskatalogBehandlingsnummerHeader(behandlingsnummer)

    val pdlBaseUrl = BaseUrl(applicationConfig.getString("pdl.baseUrl"))
        .getOrElse {
            when (it) {
                is BaseUrl.Error.InvalidFormat -> throw RuntimeException("Invalid PDL base URL: ${it.baseUrl}")
            }
        }
    val pdlHttpClient = PdlHttpClient(authHttpClient, pdlBaseUrl)

    configureMetrics()
    configureSerialization()

    routing {
        configureLivenessRoute()
        configureReadinessRoute()
        when (ktorEnv) {
            KtorEnv.LOCAL,
            KtorEnv.DEV_GCP -> configureDevRoutes(pdlHttpClient)

            KtorEnv.PROD_GCP -> {}
        }
    }
}
