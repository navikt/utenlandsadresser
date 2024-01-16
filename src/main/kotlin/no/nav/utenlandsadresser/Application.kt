package no.nav.utenlandsadresser

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.clients.http.configureAuthHttpClient
import no.nav.utenlandsadresser.clients.http.plugins.configureBehandlingskatalogBehandlingsnummerHeader
import no.nav.utenlandsadresser.clients.http.regoppslag.RegOppslagHttpClient
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.config.getApplicationConfig
import no.nav.utenlandsadresser.config.getBasicAuthConfigFromEnv
import no.nav.utenlandsadresser.config.getOAuthConfigFromEnv
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
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

    val regoppslagScope: String = applicationConfig.tryGetString("regoppslag.scope")
        ?: run {
            logger.error("regoppslag.scope not defined")
            throw IllegalStateException("regoppslag.scope not defined")
        }
    val regoppslagOAuthConfig = getOAuthConfigFromEnv(logger, regoppslagScope)
    val behandlingsnummer = BehandlingskatalogBehandlingsnummer(
        System.getenv("BEHANDLINGSKATALOG_BEHANDLINGSNUMMER")
            ?: throw IllegalStateException("Environment variable BEHANDLINGSKATALOG_BEHANDLINGSNUMMER not set")
    )
    val regoppslagAuthHttpClient = configureAuthHttpClient(regoppslagOAuthConfig)
        .configureBehandlingskatalogBehandlingsnummerHeader(behandlingsnummer)
    val regoppslagBaseUrl = applicationConfig.tryGetString("regoppslag.url")
        ?: run {
            logger.error("regoppslag.url not defined")
            throw IllegalStateException("regoppslag.url not defined")
        }
    val regOppslagClient = RegOppslagHttpClient(regoppslagAuthHttpClient, Url(regoppslagBaseUrl))

    configureMetrics()
    configureSerialization()

    routing {
        configureLivenessRoute()
        configureReadinessRoute()
        when (ktorEnv) {
            KtorEnv.LOCAL,
            KtorEnv.DEV_GCP -> configureDevRoutes(regOppslagClient)

            KtorEnv.PROD_GCP -> {}
        }
    }
}
