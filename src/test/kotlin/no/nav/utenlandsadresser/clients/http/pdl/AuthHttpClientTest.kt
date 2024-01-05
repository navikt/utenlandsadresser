package no.nav.utenlandsadresser.clients.http.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.*
import io.kotest.core.spec.style.WordSpec
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.utenlandsadresser.plugins.config.OAuthConfig
import no.nav.utenlandsadresser.plugins.configureAuthHttpClient

class AuthHttpClientTest : WordSpec({
    val mockServer = WireMockServer(0)
    register(WireMockListener.perSpec(mockServer))
    val baseUrl by lazy { mockServer.baseUrl() }

    afterTest {
        mockServer.resetAll()
    }

    "requests with auth http client" should {
        val oAuthConfig = OAuthConfig(
            tokenEndpoint = "$baseUrl/token",
            clientId = "client-id",
            clientSecret = "client-secret",
            scope = "scope",
            grantType = "client_credentials",
        ).getOrNull()!!

        "not include authorization header when client fails to fetch token" {
            mockServer.post {
                url equalTo "/token"
            } returns {
                statusCode = HttpStatusCode.InternalServerError.value
            }

            mockServer.get {
                url equalTo "/hello"
                headers contains "Authorization" notLike "Bearer.*"
            } returns {
                statusCode = HttpStatusCode.Unauthorized.value
            }

            val client = configureAuthHttpClient(oAuthConfig)
            val result = client.get("$baseUrl/hello")

            result.status shouldBe HttpStatusCode.Unauthorized
        }

        "perform request with authorization header when client successfully fetches token" {
            mockServer.post {
                url equalTo "/token"
            } returnsJson {
                // language=json
                body = """{
                    "access_token": "token",
                    "expires_in": 3600,
                    "token_type": "Bearer"
                }""".trimIndent()
            }

            mockServer.get {
                url equalTo "/hello"
                headers contains "Authorization" like "Bearer.*"
            } returns {
                statusCode = HttpStatusCode.OK.value
            }
            val client = configureAuthHttpClient(oAuthConfig)
            val result = client.get("$baseUrl/hello")

            result.status shouldBe HttpStatusCode.OK
        }

        "perform request with new authorization header when client successfully fetches new token after the old expires" {
            mockServer.post {
                url equalTo "/token"
            } returnsJson {
                // language=json
                body = """{
                    "access_token": "token",
                    "expires_in": -3600,
                    "token_type": "Bearer"
                }""".trimIndent()
            } and {
                toState = "new-token"
            }

            mockServer.post {
                whenState = "new-token"
                url equalTo "/token"
            } returnsJson {
                // language=json
                body = """{
                    "access_token": "new-token",
                    "expires_in": 3600,
                    "token_type": "Bearer"
                }""".trimIndent()
            } and {
                clearState = true
            }

            mockServer.get {
                url equalTo "/hello"
                headers contains "Authorization" equalTo "Bearer token"
            } returns {
                statusCode = HttpStatusCode.OK.value
            }

            val client = configureAuthHttpClient(oAuthConfig)
            client.get("$baseUrl/hello")

            mockServer.get {
                url equalTo "/hello"
                headers contains "Authorization" equalTo "Bearer new-token"
            } returns {
                statusCode = HttpStatusCode.OK.value
            }

            val result = client.get("$baseUrl/hello")
            result.status shouldBe HttpStatusCode.OK
        }
    }
})