package no.nav.utenlandsadresser.routes

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.plugins.configureRouting

class LivenessRouteTest : WordSpec({
    val client = specWideTestApplication {
        application {
            configureRouting()
        }
    }.client

    "GET /isalive" should {
        "respond with OK" {
            client.get("/isalive").apply {
                status shouldBe HttpStatusCode.OK
            }
        }
    }
})