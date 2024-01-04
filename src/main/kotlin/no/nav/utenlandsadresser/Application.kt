package no.nav.utenlandsadresser

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.utenlandsadresser.plugins.*
import no.nav.utenlandsadresser.plugins.config.BasicAuthConfig
import no.nav.utenlandsadresser.plugins.config.OAuthConfig
import no.nav.utenlandsadresser.plugins.security.configureBasicAuthDev
import org.slf4j.Logger
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

    val config = when (ktorEnv) {
        KtorEnv.LOCAL -> "application.conf"
        KtorEnv.DEV_GCP -> "application-dev-gcp.conf"
        KtorEnv.PROD_GCP -> "application-prod-gcp.conf"
    }.let { ConfigFactory.load(it) }

    println("Running in ${ktorEnv.name} environment")
    println("PDL URL: ${config.getString("pdl.url")}")

    // Configure basic auth for dev API
    configureBasicAuthDev(getBasicAuthConfigFromEnv(logger))

    val pdlHttpClient = configureAuthHttpClient(getOAuthConfigFromEnv(logger, config))

    configureMetrics()
    configureSerialization()
    configureRouting()
}

private fun getOAuthConfigFromEnv(logger: Logger, config: Config): OAuthConfig {
    val scope: String? = either<NonEmptyList<String>, String> {
        zipOrAccumulate(
            { ensureNotNull(config.tryGetString("pdl.cluster")) { "pdl.cluster not set in application.conf" } },
            { ensureNotNull(config.tryGetString("pdl.namespace")) { "pdl.namespace not set in application.conf" } },
            { ensureNotNull(config.tryGetString("pdl.applicationName")) { "pdl.applicationName not set in application.conf" } },
        ) { cluster, namespace, applicationName ->
            "api://$cluster.$namespace.$applicationName/.default"
        }
    }.getOrElse { errors ->
        errors.forEach {
            logger.error(it)
        }
        null
    }

    return OAuthConfig(
        clientId = System.getenv("AZURE_APP_CLIENT_ID"),
        clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET"),
        tokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        scope = scope,
        grantType = "client_credentials",
    ).getOrElse { errors ->
        errors.forEach {
            logger.error(it.toLogMessage())
        }
        throw IllegalStateException("Unable to configure OAuth for PDL client")
    }
}

private fun getBasicAuthConfigFromEnv(logger: Logger): BasicAuthConfig? {
    return BasicAuthConfig(
        username = System.getenv("DEV_API_USERNAME"),
        password = System.getenv("DEV_API_PASSWORD"),
    ).getOrElse { errors ->
        errors.forEach {
            logger.error(it.toLogMessage())
        }
        null
    }
}

private fun BasicAuthConfig.Error.toLogMessage(): String = when (this) {
    BasicAuthConfig.Error.NameMissing -> "Environment variable DEV_API_USERNAME not set"
    BasicAuthConfig.Error.PasswordMissing -> "Environment variable DEV_API_PASSWORD not set"
}

private fun OAuthConfig.Error.toLogMessage(): String = when (this) {
    OAuthConfig.Error.TokenEndpointMissing -> "Environment variable AZURE_OPENID_CONFIG_TOKEN_ENDPOINT not set"
    is OAuthConfig.Error.TokenEndpointInvalid -> "Environment variable AZURE_OPENID_CONFIG_TOKEN_ENDPOINT is invalid: $tokenEndpoint"
    OAuthConfig.Error.ClientIdMissing -> "Environment variable AZURE_APP_CLIENT_ID not set"
    OAuthConfig.Error.ClientSecretMissing -> "Environment variable AZURE_APP_CLIENT_SECRET not set"
    OAuthConfig.Error.ScopeMissing -> "Scope is missing"
    OAuthConfig.Error.GrantTypeMissing -> "Grant type is missing"
}
