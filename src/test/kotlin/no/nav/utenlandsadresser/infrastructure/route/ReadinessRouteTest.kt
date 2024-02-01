package no.nav.utenlandsadresser.infrastructure.route

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.infrastructure.route.configureReadinessRoute

class ReadinessRouteTest : WordSpec({
    val client = specWideTestApplication {
        application {
            routing {
                configureReadinessRoute()
            }
        }
    }.client

    "GET /isready" should {
        "respond with OK" {
            client.get("/isready").apply {
                status shouldBe HttpStatusCode.OK
            }
        }
    }
})
