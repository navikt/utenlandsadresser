package no.nav.utenlandsadresser.infrastructure.client.http

import com.github.tomakehurst.wiremock.client.WireMock
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.like
import com.marcinziolo.kotlin.wiremock.notLike
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import com.marcinziolo.kotlin.wiremock.returnsJson
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.utenlandsadresser.infrastructure.client.http.utils.getOAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.utils.mockOAuthToken
import no.nav.utenlandsadresser.kotest.extension.setupWiremockServer

class AuthHttpClientTest :
    WordSpec({
        val mockServer = setupWiremockServer()
        val baseUrl by lazy { mockServer.baseUrl() }
        // Ny klient per test. BearerAuthPlugin cacher tokenet, så en delt klient gjør testene avhengige av rekkefølgen.
        lateinit var client: HttpClient
        beforeTest { client = mockServer.getOAuthHttpClient() }
        afterTest { client.close() }

        "requests with auth http client" should {
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

                val result = client.get("$baseUrl/hello")

                result.status shouldBe HttpStatusCode.Unauthorized
            }

            "perform request with authorization header when client successfully fetches token" {
                mockServer.mockOAuthToken(expiresIn = 3600)

                mockServer.get {
                    url equalTo "/hello"
                    headers contains "Authorization" like "Bearer.*"
                } returns {
                    statusCode = HttpStatusCode.OK.value
                }
                val result = client.get("$baseUrl/hello")

                result.status shouldBe HttpStatusCode.OK
            }

            "perform request with new authorization header when client successfully fetches new token after the old expires" {
                // The first token expires immediately
                mockServer.post {
                    url equalTo "/token"
                    toState = "new-token"
                } returnsJson {
                    // language=json
                    body = """{"access_token": "token", "expires_in": -3600, "token_type": "Bearer"}"""
                }

                mockServer.post {
                    whenState = "new-token"
                    url equalTo "/token"
                    clearState = true
                } returnsJson {
                    // language=json
                    body = """{"access_token": "new-token", "expires_in": 3600, "token_type": "Bearer"}"""
                }

                mockServer.get {
                    url equalTo "/hello"
                    headers contains "Authorization" equalTo "Bearer token"
                } returns {
                    statusCode = HttpStatusCode.OK.value
                }

                client.get("$baseUrl/hello")

                mockServer.get {
                    url equalTo "/hello"
                    headers contains "Authorization" equalTo "Bearer new-token"
                } returns {
                    statusCode = HttpStatusCode.OK.value
                }

                val result = client.get("$baseUrl/hello")
                result.status shouldBe HttpStatusCode.OK

                // Stubben for det gamle tokenet svarer også 200, så statuskoden alene beviser ikke at tokenet ble fornyet
                mockServer.verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo("/token")))
                mockServer.verify(
                    1,
                    WireMock
                        .getRequestedFor(WireMock.urlEqualTo("/hello"))
                        .withHeader("Authorization", WireMock.equalTo("Bearer new-token")),
                )
            }
        }
    })
