package no.nav.utenlandsadresser.infrastructure.route

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.infrastructure.route.configureLivenessRoute

class LivenessRouteTest : WordSpec({
    val client = specWideTestApplication {
        application {
            routing {
                configureLivenessRoute()
            }
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