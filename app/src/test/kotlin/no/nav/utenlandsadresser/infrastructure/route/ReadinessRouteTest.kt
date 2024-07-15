package no.nav.utenlandsadresser.infrastructure.route

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication

class ReadinessRouteTest :
    WordSpec({
        val client =
            specWideTestApplication {
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
