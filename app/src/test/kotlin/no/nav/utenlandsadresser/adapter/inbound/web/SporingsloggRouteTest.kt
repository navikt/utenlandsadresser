package no.nav.utenlandsadresser.adapter.inbound.web

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
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.utenlandsadresser.adapter.inbound.web.plugin.configureSerialization
import no.nav.utenlandsadresser.application.port.inbound.SkrivSporingslogg
import no.nav.utenlandsadresser.application.port.inbound.SlettSporingslogg
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.felles.util.years
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication

class SporingsloggRouteTest :
    WordSpec({
        val skrivSporingslogg = mockk<SkrivSporingslogg>()
        val slettSporingslogg = mockk<SlettSporingslogg>()

        beforeTest { clearMocks(skrivSporingslogg, slettSporingslogg) }

        val client =
            specWideTestApplication {
                application {
                    configureSerialization()
                    routing {
                        route("/internal") {
                            configureSporingsloggRoutes(skrivSporingslogg, slettSporingslogg)
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
                coEvery { slettSporingslogg.slettEldreEnn(any()) } just runs

                val duration = 10.years.toIsoString()

                val response = client.delete("/internal/sporingslogg?olderThan=$duration")

                response.status shouldBe HttpStatusCode.OK
                coVerify(exactly = 1) { slettSporingslogg.slettEldreEnn(10.years) }
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
            }

            "return ok when sporingslogg is created" {
                coEvery { skrivSporingslogg.skriv(any(), any(), any()) } just runs
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
                    skrivSporingslogg.skriv(
                        Identitetsnummer("12345678901"),
                        Organisasjonsnummer("123456789"),
                        """{"test":"test"}""",
                    )
                }
            }
        }
    })
