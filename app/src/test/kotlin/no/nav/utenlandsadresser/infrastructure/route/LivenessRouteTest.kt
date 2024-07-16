package no.nav.utenlandsadresser.infrastructure.route

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication
import org.slf4j.LoggerFactory

class LivenessRouteTest :
    WordSpec({
        val healthCheck = mockk<HealthCheck>()
        val client =
            specWideTestApplication {
                application {
                    routing {
                        configureLivenessRoute(
                            LoggerFactory.getLogger("LivenessRouteTest"),
                            listOf(healthCheck),
                        )
                    }
                }
            }.client

        "GET /isalive" should {
            "respond with OK when everything is healthy" {
                coEvery { healthCheck.isHealthy() } returns true
                client.get("/isalive").status shouldBe HttpStatusCode.OK
            }

            "respond with InternalServerError when a health check fails" {
                coEvery { healthCheck.isHealthy() } returns false
                client.get("/isalive").status shouldBe HttpStatusCode.InternalServerError
            }
        }
    })
