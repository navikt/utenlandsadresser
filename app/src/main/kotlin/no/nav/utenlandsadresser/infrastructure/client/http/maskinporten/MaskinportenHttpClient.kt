package no.nav.utenlandsadresser.infrastructure.client.http.maskinporten

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.config.MaskinportenConfig
import no.nav.utenlandsadresser.infrastructure.client.MaskinportenClient
import no.nav.utenlandsadresser.infrastructure.client.http.maskinporten.json.MaskinportenTokenResponse
import no.nav.utenlandsadresser.infrastructure.client.http.maskinporten.json.RsaPrivateKey
import java.time.Instant
import java.util.*

class MaskinportenHttpClient(
    private val maskinportenConfig: MaskinportenConfig,
    private val httpClient: HttpClient,
) : MaskinportenClient {
    override suspend fun getAccessToken(): String {
        val clientJwk = Json.decodeFromString<RsaPrivateKey>(maskinportenConfig.clientJwk.value)

        val now = Instant.now()
        val jwtGrant =
            JWT
                .create()
                .withKeyId(clientJwk.kid)
                .withIssuer(maskinportenConfig.clientId)
                .withAudience(maskinportenConfig.tokenEndpoint)
                .withClaim("scope", maskinportenConfig.scopes)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(30))
                .withJWTId(UUID.randomUUID().toString())
                .sign(Algorithm.RSA256(clientJwk.toRSAPrivateKey()))

        val respone =
            httpClient.submitForm(
                maskinportenConfig.tokenEndpoint,
                parameters {
                    append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                    append("assertion", jwtGrant)
                },
            )

        val body =
            runCatching {
                respone.body<MaskinportenTokenResponse>()
            }.getOrElse {
                throw IllegalStateException("Unable to get access token from maskinporten: ${respone.status} ${respone.bodyAsText()}")
            }

        return body.accessToken
    }
}
