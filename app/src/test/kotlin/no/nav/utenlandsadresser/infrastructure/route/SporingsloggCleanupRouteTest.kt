package no.nav.utenlandsadresser.infrastructure.route

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresSporingsloggRepository
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.util.years

class SporingsloggCleanupRouteTest :
    WordSpec({
        val sporingsloggRepository = mockk<PostgresSporingsloggRepository>()

        val client =
            specWideTestApplication {
                application {
                    routing {
                        route("/internal") {
                            configureSporingsloggCleanupRoute(sporingsloggRepository)
                        }
                    }
                }
            }.client

        "DELETE /internal/sporingslogg" should {
            "return bad request when query param is missing" {
                val response = client.delete("/internal/sporingslogg")

                response.status shouldBe HttpStatusCode.BadRequest
            }

            "return bad request when query param is invalid" {
                val response = client.delete("/internal/sporingslogg?olderThan=NotISO8601Duration")

                response.status shouldBe HttpStatusCode.BadRequest
            }

            "return ok when query param is valid and sporingslogg older than the duration is deleted" {
                coEvery { sporingsloggRepository.deleteSporingsloggerOlderThan(any()) } returns Unit

                val duration = 10.years.toIsoString()

                val response = client.delete("/internal/sporingslogg?olderThan=$duration")

                response.status shouldBe HttpStatusCode.OK
            }
        }
    })
