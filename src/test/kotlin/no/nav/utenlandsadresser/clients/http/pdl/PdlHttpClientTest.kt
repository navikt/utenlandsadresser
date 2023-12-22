package no.nav.utenlandsadresser.clients.http.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returnsJson
import io.kotest.core.spec.style.WordSpec
import io.kotest.extensions.wiremock.WireMockListener
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*

class PdlHttpClientTest : WordSpec({
    val pdlServer = WireMockServer(0)
    listener(WireMockListener.perSpec(pdlServer))

    val httpClient = HttpClient(Apache) {
        defaultRequest {
            url {
                host = pdlServer.baseUrl()
                port = pdlServer.port()
            }
        }
    }

    /*
{
  "operationName": null,
  "variables": {
    "ident": "70078749472",
    "historikk": true
  },
  "query": "query ($ident: ID!) {\n  hentPerson(ident: $ident) {\n    kontaktadresse(historikk: false) {\n      utenlandskAdresseIFrittFormat {\n        adresselinje1\n        adresselinje2\n        adresselinje3\n        postkode\n        byEllerStedsnavn\n        landkode\n      }\n    }\n  }\n}\n"
}

     */

    "get adress" should {
        "fail when response JSON response is invalid" {
            pdlServer.post {
                url equalTo "/graphql"
            } returnsJson {
                // language=json
                body = """
                    {
                      "data": {
                        "hentPerson": {
                          "kontaktadresse": [
                            {
                              "utenlandskAdresseIFrittFormat": {
                                "adresselinje1": "1KOLEJOWA 6/5",
                                "adresselinje2": "18-500 KOLNO",
                                "adresselinje3": "CAPITAL WEST 3000",
                                "postkode": null,
                                "byEllerStedsnavn": null,
                                "landkode": "FJI"
                              }
                            }
                          ]
                        }
                      }
                    }
                """.trimIndent()
            }
        }
    }

})
