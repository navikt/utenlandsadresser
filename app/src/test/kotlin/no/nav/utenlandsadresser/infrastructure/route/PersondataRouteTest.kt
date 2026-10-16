package no.nav.utenlandsadresser.infrastructure.route

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.mockk.mockk
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.plugin.configureSerialization
import no.nav.utenlandsadresser.plugin.maskinporten.configureMaskinportenAuthentication
import no.nav.utenlandsadresser.plugin.maskinporten.validateOrganisasjonsnummer

class PersondataRouteTest :
    WordSpec({
        val issuer = Issuer("https://maskinporten.no")
        val postadresseScope = Scope("nav:utenlandsadresser:postadresse.read")
        val utenlandskIdScope = Scope("nav:utenlandsadresser:utenlandskid.read")
        val organisasjonsnummer = Organisasjonsnummer("974761076")

        fun token(scope: Scope): String =
            JWT
                .create()
                .withIssuer(issuer.value)
                .withClaim("scope", scope.value)
                .withClaim("consumer", mapOf("ID" to "0192:${organisasjonsnummer.value}"))
                .sign(Algorithm.none())

        val postadresseToken = token(postadresseScope)
        val utenlandskIdToken = token(utenlandskIdScope)

        val requests =
            listOf(
                "/api/v2/persondata/abonnement/start" to """{"identitetsnummer":"12345678910"}""",
                "/api/v2/persondata/abonnement/stopp" to """{"abonnementId":"f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f"}""",
                "/api/v2/persondata/feed" to """{"løpenummer":"1"}""",
                "/api/v3/persondata/abonnement/start" to """{"identitetsnummer":"12345678910"}""",
                "/api/v3/persondata/abonnement/stopp" to """{"abonnementId":"f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f"}""",
                "/api/v3/persondata/postadresse/feed" to """{"løpenummer":"1"}""",
                "/api/v3/persondata/utenlandskid/feed" to """{"løpenummer":"1"}""",
            )

        val client =
            specWideTestApplication {
                application {
                    configureSerialization()
                    for ((name, scope) in listOf(
                        POSTADRESSE_MASKINPORTEN_AUTH to postadresseScope,
                        UTENLANDSK_ID_MASKINPORTEN_AUTH to utenlandskIdScope,
                    )) {
                        configureMaskinportenAuthentication(
                            configurationName = name,
                            issuer = issuer,
                            requiredScopes = setOf(scope),
                            jwkProvider = mockk(),
                            jwtConfigBlock = { verifier { JWT.require(Algorithm.none()).build() } },
                            jwtValidationBlock = validateOrganisasjonsnummer(listOf(organisasjonsnummer.value)),
                        )
                    }
                    routing {
                        configurePersondataRoute()
                        configurePersondataV3Route()
                    }
                }
            }.client

        "persondata routes" should {
            "return unauthorized without a token for all endpoints" {
                requests.forEach { (path, body) ->
                    val response =
                        client.post(path) {
                            contentType(ContentType.Application.Json)
                            setBody(body)
                        }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            "return unauthorized with only the utenlandsk id scope for all endpoints" {
                requests.forEach { (path, body) ->
                    val response =
                        client.post(path) {
                            bearerAuth(utenlandskIdToken)
                            contentType(ContentType.Application.Json)
                            setBody(body)
                        }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            "accept a token with the postadresse scope for all endpoints" {
                requests.forEach { (path, body) ->
                    val response =
                        client.post(path) {
                            bearerAuth(postadresseToken)
                            contentType(ContentType.Application.Json)
                            setBody(body)
                        }

                    val expectedStatus =
                        if (path.endsWith("/abonnement/start")) {
                            HttpStatusCode.Created
                        } else {
                            HttpStatusCode.OK
                        }
                    response.status shouldBe expectedStatus
                }
            }
        }
    })
