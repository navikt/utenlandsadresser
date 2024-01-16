package no.nav.utenlandsadresser.clients.http

import arrow.core.Either
import arrow.core.raise.either
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.clients.http.plugins.BearerAuthPlugin
import no.nav.utenlandsadresser.plugins.config.OAuthConfig

fun configureAuthHttpClient(
    oAuthConfig: OAuthConfig,
    tokenClient: HttpClient = HttpClient(CIO) {
        // TODO: Remove
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
): HttpClient = HttpClient(CIO) {
    // TODO: Remove
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }

    install(BearerAuthPlugin) {
        this.oAuthConfig = oAuthConfig
        this.tokenClient = tokenClient
    }
}

suspend fun fetchToken(
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
        ).body<TokenInfo>()
    }.getOrElse {
        when (it) {
            is NoTransformationFoundException -> raise(FetchTokenError.NoMatchingJsonFound)
            else -> throw it
        }
    }
}

sealed class FetchTokenError {
    data object NoMatchingJsonFound : FetchTokenError()

}


@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
)
