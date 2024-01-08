package no.nav.utenlandsadresser.clients.http

import com.marcinziolo.kotlin.wiremock.*
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotest.extension.setupWiremockServer
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.clients.http.plugins.configureBehandlingskatalogBehandlingsnummerHeader

class BehandlingsnummerHttpClientTest : WordSpec({
    val mockServer = setupWiremockServer()
    val baseUrl by lazy { mockServer.baseUrl() }

    "http client with configured behandlingsnummer header" should {
        val behandlingsnummer = BehandlingskatalogBehandlingsnummer("B123")
        val client = HttpClient(CIO)
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