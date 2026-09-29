package no.nav.utenlandsadresser.infrastructure.client.http.pdl

import arrow.core.left
import arrow.core.right
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.headersOf
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Iso3166Alpha3
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetKilde
import no.nav.utenlandsadresser.infrastructure.client.HentUtenlandskId
import java.net.URI

class PdlGraphQLClientTest :
    WordSpec({
        "hentUtenlandskIdentitet" should {
            "map active identities and use the latest create or correction source" {
                val httpClient =
                    HttpClient(
                        MockEngine { request ->
                            request.headers[HttpHeaders.Authorization] shouldBe null
                            request.headers["Behandlingsnummer"] shouldBe "test-behandlingsnummer"
                            request.headers["Nav-Call-Id"].isNullOrBlank() shouldBe false

                            respond(
                                content =
                                    """
                                    {
                                      "data": {
                                        "hentPerson": {
                                          "utenlandskIdentifikasjonsnummer": [
                                            {
                                              "identifikasjonsnummer": "foreign-id-1",
                                              "utstederland": "HRV",
                                              "opphoert": false,
                                              "metadata": {
                                                "endringer": [
                                                  {
                                                    "hendelseId": "event-1",
                                                    "kilde": "Første kilde",
                                                    "registrert": "2024-01-26T08:40:54",
                                                    "registrertAv": "system",
                                                    "systemkilde": "pdl-web",
                                                    "type": "OPPRETT"
                                                  },
                                                  {
                                                    "hendelseId": "event-2",
                                                    "kilde": "Ny kilde",
                                                    "registrert": "2024-02-26T08:40:54",
                                                    "registrertAv": "system",
                                                    "systemkilde": "pdl-web",
                                                    "type": "KORRIGER"
                                                  }
                                                ],
                                                "historisk": false,
                                                "master": "PDL"
                                              }
                                            },
                                            {
                                              "identifikasjonsnummer": "foreign-id-2",
                                              "utstederland": "USA",
                                              "opphoert": true,
                                              "metadata": {
                                                "endringer": [],
                                                "historisk": false,
                                                "master": "PDL"
                                              }
                                            }
                                          ]
                                        }
                                      }
                                    }
                                    """.trimIndent(),
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                            )
                        },
                    )
                val client = pdlClient(httpClient)

                val result = client.hentUtenlandskIdentitet(Identitetsnummer("test-ident"))

                result shouldBe
                    listOf(
                        UtenlandskIdentitet(
                            identitetsnummer = UtenlandskIdentitetsnummer("foreign-id-1"),
                            utstederland = Iso3166Alpha3("HRV"),
                            kilde = UtenlandskIdentitetKilde("Ny kilde"),
                        ),
                    ).right()
                httpClient.close()
            }

            "return an error when PDL returns GraphQL errors" {
                val httpClient =
                    HttpClient(
                        MockEngine {
                            respond(
                                content = """{"errors":[{"message":"query failed","path":["hentPerson"]}]}""",
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                            )
                        },
                    )
                val client = pdlClient(httpClient)

                val result = client.hentUtenlandskIdentitet(Identitetsnummer("test-ident"))

                result shouldBe HentUtenlandskId.Error.FeilIRespons.left()
                httpClient.close()
            }

            "return an error when the PDL request fails" {
                val httpClient =
                    HttpClient(
                        MockEngine {
                            throw IllegalStateException("network failure")
                        },
                    )
                val client = pdlClient(httpClient)

                val result = client.hentUtenlandskIdentitet(Identitetsnummer("test-ident"))

                result shouldBe HentUtenlandskId.Error.Kommunikasjonsfeil.left()
                httpClient.close()
            }

            "return a specific error for an invalid issuer country" {
                val (httpClient, client) = clientWithResponse(activeIdentityResponse(utstederland = "XXX"))

                val result = client.hentUtenlandskIdentitet(Identitetsnummer("test-ident"))

                result shouldBe HentUtenlandskId.Error.UgyldigUtstederland.left()
                httpClient.close()
            }

            "return a specific error when the source is missing" {
                val (httpClient, client) = clientWithResponse(activeIdentityResponse(utstederland = "HRV"))

                val result = client.hentUtenlandskIdentitet(Identitetsnummer("test-ident"))

                result shouldBe HentUtenlandskId.Error.ManglerKildeForUtenlandskIdentitet.left()
                httpClient.close()
            }

            "ignore identities where PDL is not the master" {
                val (httpClient, client) =
                    clientWithResponse(
                        activeIdentityResponse(
                            utstederland = "HRV",
                            master = "FREG",
                        ),
                    )

                val result = client.hentUtenlandskIdentitet(Identitetsnummer("test-ident"))

                result shouldBe emptyList<UtenlandskIdentitet>().right()
                httpClient.close()
            }
        }
    })

private fun clientWithResponse(response: String): Pair<HttpClient, PdlGraphQLClient> {
    val httpClient =
        HttpClient(
            MockEngine {
                respond(
                    content = response,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )
    return httpClient to
        pdlClient(httpClient)
}

private fun pdlClient(httpClient: HttpClient) =
    PdlGraphQLClient(
        graphQLClient =
            GraphQLKtorClient(
                url = URI.create("https://pdl.test/graphql").toURL(),
                httpClient = httpClient,
            ),
        behandlingsnummer = BehandlingskatalogBehandlingsnummer("test-behandlingsnummer"),
    )

private fun activeIdentityResponse(
    utstederland: String,
    master: String = "PDL",
) =
    """
    {
      "data": {
        "hentPerson": {
          "utenlandskIdentifikasjonsnummer": [
            {
              "identifikasjonsnummer": "foreign-id",
              "utstederland": "$utstederland",
              "opphoert": false,
              "metadata": {
                "endringer": [],
                "historisk": false,
                "master": "$master"
              }
            }
          ]
        }
      }
    }
    """.trimIndent()
