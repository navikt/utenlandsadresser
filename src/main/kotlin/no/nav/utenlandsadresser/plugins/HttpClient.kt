package no.nav.utenlandsadresser.plugins

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.BearerToken
import no.nav.utenlandsadresser.plugins.config.OAuthConfig
import org.slf4j.LoggerFactory
import java.time.Instant

class BearerAuthConfig {
    var oAuthConfig: OAuthConfig? = null
    var tokenClient: HttpClient? = null
}

val BearerAuth = createClientPlugin("BearerAuth", ::BearerAuthConfig) {
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

fun configureAuthHttpClient(
    oAuthConfig: OAuthConfig,
    tokenClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
): HttpClient = HttpClient(CIO) {
    // TODO: Remove
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.HEADERS
    }
    install(ContentNegotiation) {
        json()
    }

    install(BearerAuth) {
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
