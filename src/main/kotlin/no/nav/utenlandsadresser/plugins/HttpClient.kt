package no.nav.utenlandsadresser.plugins

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.plugins.config.OAuthConfig

fun configureAuthHttpClient(
    oAuthConfig: OAuthConfig,
): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
    install(Auth) {
        bearer {
            var bearerToken: BearerTokens? = null

            loadTokens { bearerToken }
            refreshTokens {
                val tokenInfo = client.submitForm(
                    oAuthConfig.tokenEndpoint.toString(),
                    parameters {
                        append("client_id", oAuthConfig.clientId.value)
                        append("client_secret", oAuthConfig.clientSecret.value)
                        append("scope", oAuthConfig.scope.value)
                        append("grant_type", oAuthConfig.grantType.value)
                    },
                ).body<TokenInfo>()

                BearerTokens(accessToken = tokenInfo.accessToken, refreshToken = tokenInfo.refreshToken)
                    .also { bearerToken = it }
            }
        }
    }
}

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
)
