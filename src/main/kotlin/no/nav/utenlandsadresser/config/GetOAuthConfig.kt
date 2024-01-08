package no.nav.utenlandsadresser.config

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import com.typesafe.config.Config
import io.ktor.server.config.*
import no.nav.utenlandsadresser.plugins.config.OAuthConfig
import org.slf4j.Logger

fun getOAuthConfigFromEnv(logger: Logger, config: Config): OAuthConfig {
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

private fun OAuthConfig.Error.toLogMessage(): String = when (this) {
    OAuthConfig.Error.TokenEndpointMissing -> "Environment variable AZURE_OPENID_CONFIG_TOKEN_ENDPOINT not set"
    is OAuthConfig.Error.TokenEndpointInvalid -> "Environment variable AZURE_OPENID_CONFIG_TOKEN_ENDPOINT is invalid: $tokenEndpoint"
    OAuthConfig.Error.ClientIdMissing -> "Environment variable AZURE_APP_CLIENT_ID not set"
    OAuthConfig.Error.ClientSecretMissing -> "Environment variable AZURE_APP_CLIENT_SECRET not set"
    OAuthConfig.Error.ScopeMissing -> "Scope is missing"
    OAuthConfig.Error.GrantTypeMissing -> "Grant type is missing"
}