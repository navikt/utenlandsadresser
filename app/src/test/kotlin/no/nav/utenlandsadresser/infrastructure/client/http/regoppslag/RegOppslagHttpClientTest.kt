package no.nav.utenlandsadresser.infrastructure.client.http.regoppslag

import arrow.core.getOrElse
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.like
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import com.marcinziolo.kotlin.wiremock.returnsJson
import io.kotest.assertions.fail
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.RegisteroppslagHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.utils.getOAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.utils.mockOAuthToken
import no.nav.utenlandsadresser.kotest.extension.setupWiremockServer

class RegOppslagHttpClientTest :
    WordSpec({
        val mockServer = setupWiremockServer()
        val baseUrl by lazy { mockServer.baseUrl() }
        val bearerClient by lazy { mockServer.getOAuthHttpClient() }
        val identitetsnummer = Identitetsnummer("99999912345")
        val regOppslagHttpClient by lazy {
            RegisteroppslagHttpClient(
                bearerClient,
                Url(baseUrl),
                BehandlingskatalogBehandlingsnummer("1"),
            )
        }

        beforeTest { mockServer.mockOAuthToken() }

        "succesfull requests with regoppslag http client" should {
            "return norsk postadresse" {
                mockServer.post {
                    url equalTo "/rest/postadresse"
                    headers contains "Behandlingsnummer" like "1"
                    // language=json
                    body equalTo
                        """
                    |{
                    |  "ident": "${identitetsnummer.value}",
                    |  "filtrerAdressebeskyttelse": [
                    |    "strengt_fortrolig_utland",
                    |    "strengt_fortrolig",
                    |    "fortrolig"
                    |  ]
                    |}
                        """.trimMargin()
                    headers contains "Authorization" like "Bearer.*"
                } returnsJson {
                    // language=json
                    body =
                        """
                        {
                          "navn": "Test Testesen",
                          "adresse": {
                            "adresseKilde": "Bostedsadresse",
                            "type": "NORSKPOSTADRESSE",
                            "adresselinje1": "Postboks 5 St Olavs Plass",
                            "adresselinje2": null,
                            "adresselinje3": null,
                            "postnummer": "0130",
                            "poststed": "OSLO",
                            "landkode": "NO",
                            "land": "Norge"
                          }
                        }
                        """.trimIndent()
                }

                regOppslagHttpClient
                    .getPostadresse(identitetsnummer)
                    .getOrElse { fail(it.toString()) }
                    .shouldBeTypeOf<Postadresse.Norsk>()
            }

            "return utenlandsk postadresse" {
                mockServer.post {
                    url equalTo "/rest/postadresse"
                    headers contains "Behandlingsnummer" like "1"
                    // language=json
                    body equalTo
                        """
                    |{
                    |  "ident": "${identitetsnummer.value}",
                    |  "filtrerAdressebeskyttelse": [
                    |    "strengt_fortrolig_utland",
                    |    "strengt_fortrolig",
                    |    "fortrolig"
                    |  ]
                    |}
                        """.trimMargin()
                    headers contains "Authorization" like "Bearer.*"
                } returnsJson {
                    // language=json
                    body =
                        """
                        {
                          "navn": "ARBEIDS- OG VELFERDSETATEN",
                          "adresse": {
                            "adresseKilde": "Bostedsadresse",
                            "type": "UTENLANDSKPOSTADRESSE",
                            "adresselinje1": "adresselinje 1",
                            "adresselinje2": "adresselinje 2",
                            "adresselinje3": "adresselinje 3",
                            "postnummer": null,
                            "poststed": null,
                            "landkode": "DK",
                            "land": "Denmark"
                          }
                        }
                        """.trimIndent()
                }

                regOppslagHttpClient
                    .getPostadresse(identitetsnummer)
                    .getOrElse { fail(it.toString()) }
                    .shouldBeTypeOf<Postadresse.Utenlandsk>()
            }

            "return unknown when utenlandsadresse is filtered out" {
                mockServer.post {
                    url equalTo "/rest/postadresse"
                    headers contains "Behandlingsnummer" like "1"
                    // language=json
                    body equalTo
                        """
                    |{
                    |  "ident": "${identitetsnummer.value}",
                    |  "filtrerAdressebeskyttelse": [
                    |    "strengt_fortrolig_utland",
                    |    "strengt_fortrolig",
                    |    "fortrolig"
                    |  ]
                    |}
                        """.trimMargin()
                    headers contains "Authorization" like "Bearer.*"
                } returns {
                    statusCode = HttpStatusCode.NoContent.value
                }

                regOppslagHttpClient
                    .getPostadresse(identitetsnummer)
                    .leftOrNull()
                    .shouldBeTypeOf<GetPostadresseError.UkjentAdresse>()
            }
        }

        "unsuccesfull requests with regoppslag http client" should {
            "return error when response status is 400" {
                mockServer.post {
                    url equalTo "/rest/postadresse"
                } returns {
                    statusCode = HttpStatusCode.BadRequest.value
                }

                regOppslagHttpClient
                    .getPostadresse(identitetsnummer)
                    .leftOrNull()
                    .shouldBeTypeOf<GetPostadresseError.UgyldigForespørsel>()
            }

            "return ingen tilgang error when response status is 401" {
                mockServer.post {
                    url equalTo "/rest/postadresse"
                } returns {
                    statusCode = HttpStatusCode.Unauthorized.value
                }

                regOppslagHttpClient
                    .getPostadresse(identitetsnummer)
                    .leftOrNull()
                    .shouldBeTypeOf<GetPostadresseError.IngenTilgang>()
            }

            "return ukjent adresse error when response status is 404" {
                mockServer.post {
                    url equalTo "/rest/postadresse"
                } returns {
                    statusCode = HttpStatusCode.NotFound.value
                }

                regOppslagHttpClient
                    .getPostadresse(identitetsnummer)
                    .leftOrNull()
                    .shouldBeTypeOf<GetPostadresseError.UkjentAdresse>()
            }

            "return teknisk error when response status is 500" {
                mockServer.post {
                    url equalTo "/rest/postadresse"
                } returns {
                    statusCode = HttpStatusCode.InternalServerError.value
                }

                regOppslagHttpClient
                    .getPostadresse(identitetsnummer)
                    .leftOrNull()
                    .shouldBeTypeOf<GetPostadresseError.UkjentFeil>()
            }
        }
    })
