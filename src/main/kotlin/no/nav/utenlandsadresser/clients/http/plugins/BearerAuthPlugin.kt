package no.nav.utenlandsadresser.clients.http.plugins

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.BearerToken
import no.nav.utenlandsadresser.clients.http.plugins.config.OAuthConfig
import org.slf4j.LoggerFactory
import java.time.Instant

val BearerAuthPlugin = createClientPlugin("BearerAuth", ::BearerAuthConfig) {
    var bearerToken: BearerToken? = null
    var tokenExpiryTime = Instant.MIN
    val logger = LoggerFactory.getLogger("BearerAuth")
    val oAuthConfig = pluginConfig.oAuthConfig!!
    val tokenClient = pluginConfig.tokenClient!!

    onRequest { request, _ ->
        if (bearerToken == null || Instant.now().isAfter(tokenExpiryTime)) {
            val tokenInfo = fetchToken(tokenClient, oAuthConfig)
                .getOrElse { error ->
                    when (error) {
                        FetchTokenError.NoMatchingJsonFound -> logger.error("Unable to fetch token: $error")
                        is FetchTokenError.HttpError -> logger.error("Unable to fetch token: ${error.statusCode} ${error.body}")
                    }
                    return@onRequest
                }

            tokenExpiryTime = Instant.now().plusSeconds(tokenInfo.expiresIn.toLong())
            bearerToken = BearerToken(tokenInfo.accessToken)
        }

        bearerToken?.let {
            request.headers.append(HttpHeaders.Authorization, "Bearer ${it.value}")
        }
    }
}

class BearerAuthConfig {
    var oAuthConfig: OAuthConfig? = null
    var tokenClient: HttpClient? = null
}

private suspend fun fetchToken(
    client: HttpClient,
    oAuthConfig: OAuthConfig
): Either<FetchTokenError, TokenInfo> = either {
    runCatching {
        client.submitForm(
            oAuthConfig.tokenEndpoint.toString(),
            parameters {
                append("client_id", oAuthConfig.clientId.value)
                append("client_secret", oAuthConfig.clientSecret.value)
                append("scope", oAuthConfig.scope.value)
                append("grant_type", oAuthConfig.grantType.value)
            },
        ).let {
            when (it.status) {
                HttpStatusCode.OK -> it.body<TokenInfo>()
                else -> raise(FetchTokenError.HttpError(it.status, it.bodyAsText()))
            }
        }
    }.getOrElse {
        when (it) {
            is NoTransformationFoundException -> raise(FetchTokenError.NoMatchingJsonFound)
            else -> throw it
        }
    }
}

sealed class FetchTokenError {
    data object NoMatchingJsonFound : FetchTokenError()
    data class HttpError(val statusCode: HttpStatusCode, val body: String) : FetchTokenError()
}

@Serializable
private data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
)