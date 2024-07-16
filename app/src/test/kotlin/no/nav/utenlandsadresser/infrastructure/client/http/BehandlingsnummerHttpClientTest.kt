package no.nav.utenlandsadresser.infrastructure.client.http

import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.like
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.configureBehandlingskatalogBehandlingsnummerHeader
import no.nav.utenlandsadresser.kotest.extension.setupWiremockServer

class BehandlingsnummerHttpClientTest :
    WordSpec({
        val mockServer = setupWiremockServer()
        val baseUrl by lazy { mockServer.baseUrl() }

        "http client with configured behandlingsnummer header" should {
            val behandlingsnummer = BehandlingskatalogBehandlingsnummer("B123")
            val client =
                HttpClient(CIO)
                    .configureBehandlingskatalogBehandlingsnummerHeader(behandlingsnummer)

            "perform request with behandlingsnummer header" {
                mockServer.get {
                    url equalTo "/hello"
                    headers contains "Behandlingsnummer" like "B123"
                } returns {
                    statusCode = 200
                }
                val result = client.get("$baseUrl/hello")
                result.status.value shouldBe 200
            }
        }
    })
