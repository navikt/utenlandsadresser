package no.nav.utenlandsadresser.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.mockk
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.plugin.maskinporten.OrganisasjonsnummerKey
import no.nav.utenlandsadresser.plugin.maskinporten.configureMaskinportenAuthentication
import no.nav.utenlandsadresser.plugin.maskinporten.validateOrganisasjonsnummer

class VerifyScopeTest :
    WordSpec({
        val scope = Scope("test-scope")
        val organisasjonsnummer = Organisasjonsnummer("889640782")
        val issuer = Issuer("https://maskinporten.no")
        val client =
            specWideTestApplication {
                application {
                    configureMaskinportenAuthentication(
                        configurationName = "test-maskinporten",
                        issuer = issuer,
                        requiredScopes = setOf(scope),
                        jwkProvider = mockk(),
                        jwtConfigBlock = { verifier { JWT.require(Algorithm.none()).build() } },
                        jwtValidationBlock = validateOrganisasjonsnummer(listOf(organisasjonsnummer.value)),
                    )

                    routing {
                        authenticate("test-maskinporten") {
                            route("/test") {
                                get("/hello") {
                                    // Verify the organisasjonsnummer is assigned to call attributes
                                    call.attributes[OrganisasjonsnummerKey] shouldBe organisasjonsnummer.value

                                    call.respond("Hello")
                                }
                            }
                        }
                    }
                }
            }.client

        "GET /test/hello" should {
            "respond with UNAUTHORIZED when no jwt is provided" {
                val response = client.get("/test/hello")

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when no scope is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(
                            JWT
                                .create()
                                .withIssuer(issuer.value)
                                .sign(Algorithm.none()),
                        )
                    }

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when wrong scope is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(
                            JWT
                                .create()
                                .withClaim("scope", "wrong-scope")
                                .withIssuer(issuer.value)
                                .sign(Algorithm.none()),
                        )
                    }

                response.status.value shouldBe 401
            }

            "respond with UNAUTHORIZED when no consumer.ID claim is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(
                            JWT
                                .create()
                                .withClaim("scope", scope.value)
                                .withIssuer(issuer.value)
                                .sign(Algorithm.none()),
                        )
                    }

                response.status.value shouldBe 401
            }

            "respond with OK when correct jwt is provided" {
                val response =
                    client.get("/test/hello") {
                        bearerAuth(
                            JWT
                                .create()
                                .withClaim("scope", scope.value)
                                .withClaim("consumer", mapOf("ID" to "0192:889640782"))
                                .withIssuer(issuer.value)
                                .sign(Algorithm.none()),
                        )
                    }

                response.status.value shouldBe 200
            }
        }
    })
