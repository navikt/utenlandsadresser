package no.nav.utenlandsadresser.infrastructure.client.http.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returnsJson
import io.ktor.client.*
import no.nav.utenlandsadresser.infrastructure.client.http.configureAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig

fun WireMockServer.getOAuthHttpClient(): HttpClient {
    val oAuthConfig = OAuthConfig(
        tokenEndpoint = "${baseUrl()}/token",
        clientId = "client-id",
        clientSecret = "client-secret",
        scope = "scope",
        grantType = "client_credentials",
    ).getOrNull()!!
    return configureAuthHttpClient(oAuthConfig)
}

fun WireMockServer.mockOAuthToken(token: String = "token", expiresIn: Int = 3600) {
    post {
        url equalTo "/token"
    } returnsJson {
        // language=json
        body = """{"access_token": "$token", "expires_in": $expiresIn, "token_type": "Bearer"}"""
    }
}