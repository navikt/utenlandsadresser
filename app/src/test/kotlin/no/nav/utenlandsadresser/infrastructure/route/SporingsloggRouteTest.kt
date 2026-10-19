package no.nav.utenlandsadresser.infrastructure.route

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import no.nav.utenlandsadresser.FastClock
import no.nav.utenlandsadresser.app.SporingsloggRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.plugin.configureSerialization
import no.nav.utenlandsadresser.util.years

class SporingsloggRouteTest :
    WordSpec({
        val sporingsloggRepository = mockk<SporingsloggRepository>()
        val fastClock = FastClock()

        val client =
            specWideTestApplication {
                application {
                    configureSerialization()
                    routing {
                        route("/internal") {
                            configureSporingsloggRoutes(sporingsloggRepository, fastClock)
                        }
                    }
                }
            }.client

        beforeTest {
            clearMocks(sporingsloggRepository)
        }

        "DELETE /internal/sporingslogg" should {
            "return bad request when query param is missing" {
                val response = client.delete("/internal/sporingslogg")

                response.status shouldBe HttpStatusCode.BadRequest
                coVerify(exactly = 0) { sporingsloggRepository.deleteSporingsloggerOlderThan(any()) }
            }

            "return bad request when query param is invalid" {
                val response = client.delete("/internal/sporingslogg?olderThan=NotISO8601Duration")

                response.status shouldBe HttpStatusCode.BadRequest
                coVerify(exactly = 0) { sporingsloggRepository.deleteSporingsloggerOlderThan(any()) }
            }

            "return ok when query param is valid and sporingslogg older than the duration is deleted" {
                coEvery { sporingsloggRepository.deleteSporingsloggerOlderThan(any()) } returns Unit

                val duration = 10.years.toIsoString()

                val response = client.delete("/internal/sporingslogg?olderThan=$duration")

                response.status shouldBe HttpStatusCode.OK
                coVerify(exactly = 1) { sporingsloggRepository.deleteSporingsloggerOlderThan(10.years) }
            }
        }

        "POST /internal/sporingslogg" should {
            "return bad request when request body is invalid" {
                val response =
                    client.post("/internal/sporingslogg") {
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"invalid": "json"}""")
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                coVerify(exactly = 0) { sporingsloggRepository.loggJson(any(), any(), any(), any()) }
            }

            "return ok when sporingslogg is created" {
                coEvery { sporingsloggRepository.loggJson(any(), any(), any(), any()) } returns Unit
                val response =
                    client.post("/internal/sporingslogg") {
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody(
                            """
                            {
                                "identitetsnummer": "12345678901",
                                "organisasjonsnummer": "123456789",
                                "dataTilLogging": {
                                    "test": "test"
                                }
                            }
                            """.trimIndent(),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                coVerify(exactly = 1) {
                    sporingsloggRepository.loggJson(
                        identitetsnummer = Identitetsnummer("12345678901"),
                        organisasjonsnummer = Organisasjonsnummer("123456789"),
                        json = buildJsonObject { put("test", "test") },
                        tidspunktForUtlevering = fastClock.nå,
                    )
                }
            }
        }
    })
