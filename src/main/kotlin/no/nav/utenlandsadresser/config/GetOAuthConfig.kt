package no.nav.utenlandsadresser.config

import arrow.core.getOrElse
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig
import org.slf4j.Logger

fun getOAuthConfigFromEnv(scope: String?, logger: Logger): OAuthConfig = OAuthConfig(
    clientId = System.getenv("AZURE_APP_CLIENT_ID"),
    clientSecret = System.getenv("AZURE_APP_CLIENT_SECRET"),
    tokenEndpoint = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    scope = scope,
    grantType = "client_credentials",
).getOrElse { errors ->
    errors.forEach {
        logger.error(it.toLogMessage())
    }
    throw IllegalStateException("Unable to create OAuthConfig")
}

private fun OAuthConfig.Error.toLogMessage(): String = when (this) {
    OAuthConfig.Error.TokenEndpointMissing -> "Environment variable AZURE_OPENID_CONFIG_TOKEN_ENDPOINT not set"
    is OAuthConfig.Error.TokenEndpointInvalid -> "Environment variable AZURE_OPENID_CONFIG_TOKEN_ENDPOINT is invalid: $tokenEndpoint"
    OAuthConfig.Error.ClientIdMissing -> "Environment variable AZURE_APP_CLIENT_ID not set"
    OAuthConfig.Error.ClientSecretMissing -> "Environment variable AZURE_APP_CLIENT_SECRET not set"
    OAuthConfig.Error.ScopeMissing -> "Scope is missing"
    OAuthConfig.Error.GrantTypeMissing -> "Grant type is missing"
}