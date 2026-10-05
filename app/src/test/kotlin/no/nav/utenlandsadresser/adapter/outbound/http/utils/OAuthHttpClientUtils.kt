package no.nav.utenlandsadresser.adapter.outbound.http.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returnsJson
import com.sksamuel.hoplite.Masked
import io.ktor.client.HttpClient
import no.nav.utenlandsadresser.felles.auth.OAuthConfig
import no.nav.utenlandsadresser.felles.auth.Scope
import no.nav.utenlandsadresser.felles.http.createAuthHttpClient

fun WireMockServer.getOAuthHttpClient(): HttpClient {
    val oAuthConfig =
        OAuthConfig(
            tokenEndpoint = "${baseUrl()}/token",
            clientId = "client-id",
            clientSecret = Masked("client-secret"),
            grantType = "client_credentials",
        )
    return createAuthHttpClient(oAuthConfig, listOf(Scope("scope")))
}

fun WireMockServer.mockOAuthToken(
    token: String = "token",
    expiresIn: Int = 3600,
) {
    post {
        url equalTo "/token"
    } returnsJson {
        // language=json
        body = """{"access_token": "$token", "expires_in": $expiresIn, "token_type": "Bearer"}"""
    }
}
