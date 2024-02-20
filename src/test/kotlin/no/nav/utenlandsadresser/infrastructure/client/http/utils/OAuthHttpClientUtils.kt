package no.nav.utenlandsadresser.infrastructure.client.http.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returnsJson
import com.sksamuel.hoplite.Masked
import io.ktor.client.*
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.client.http.configureAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig

fun WireMockServer.getOAuthHttpClient(): HttpClient {
    val oAuthConfig = OAuthConfig(
        tokenEndpoint = "${baseUrl()}/token",
        clientId = "client-id",
        clientSecret = Masked("client-secret"),
        grantType = "client_credentials",
    )
    return configureAuthHttpClient(oAuthConfig, listOf(Scope("scope")))
}

fun WireMockServer.mockOAuthToken(token: String = "token", expiresIn: Int = 3600) {
    post {
        url equalTo "/token"
    } returnsJson {
        // language=json
        body = """{"access_token": "$token", "expires_in": $expiresIn, "token_type": "Bearer"}"""
    }
}