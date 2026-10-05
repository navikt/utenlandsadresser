package no.nav.utenlandsadresser.adapter.inbound.web

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.utenlandsadresser.adapter.inbound.web.plugin.configureSerialization
import no.nav.utenlandsadresser.adapter.outbound.maskinporten.MaskinportenClient
import no.nav.utenlandsadresser.application.port.outbound.PostadresseOppslag
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdOppslag
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Iso3166Alpha3
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetKilde
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetsnummer

class DevRouteTest :
    WordSpec({
        val postadresseOppslag = mockk<PostadresseOppslag>()
        val maskinportenClient = mockk<MaskinportenClient>()
        val utenlandskIdOppslag = mockk<UtenlandskIdOppslag>()
        val identitetsnummer = Identitetsnummer("syntetisk-testident")
        val identitet =
            UtenlandskIdentitet(
                identitetsnummer = UtenlandskIdentitetsnummer("foreign-test-id"),
                utstederland = Iso3166Alpha3("HRV"),
                kilde = UtenlandskIdentitetKilde("Testkilde"),
            )

        "POST /internal/dev/pdl/utenlandsk-id" should {
            "return active foreign identities from the client" {
                coEvery { utenlandskIdOppslag.hentUtenlandskIdentitet(identitetsnummer) } returns
                    listOf(identitet).right()

                testApplication {
                    application {
                        configureSerialization()
                        routing {
                            route("/internal") {
                                configureDevRoutes(postadresseOppslag, maskinportenClient, utenlandskIdOppslag)
                            }
                        }
                    }

                    val response =
                        client.post("/internal/dev/pdl/utenlandsk-id") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"identitetsnummer":"syntetisk-testident"}""")
                        }

                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldEqualJson
                        """[{"identitetsnummer":"foreign-test-id","utstederland":"HRV","kilde":"Testkilde"}]"""
                }
            }

            "return a generic upstream error without exposing client error details" {
                coEvery { utenlandskIdOppslag.hentUtenlandskIdentitet(identitetsnummer) } returns
                    UtenlandskIdOppslag.Error.Kommunikasjonsfeil.left()

                testApplication {
                    application {
                        configureSerialization()
                        routing {
                            route("/internal") {
                                configureDevRoutes(postadresseOppslag, maskinportenClient, utenlandskIdOppslag)
                            }
                        }
                    }

                    val response =
                        client.post("/internal/dev/pdl/utenlandsk-id") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"identitetsnummer":"syntetisk-testident"}""")
                        }

                    response.status shouldBe HttpStatusCode.BadGateway
                    response.bodyAsText() shouldBe "Feil ved oppslag av utenlandsk identitet"
                }
            }
        }
    })
