package no.nav.utenlandsadresser.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.plugin.maskinporten.OrganisasjonsnummerKey
import no.nav.utenlandsadresser.plugin.maskinporten.protectWithOrganisasjonsnummer
import no.nav.utenlandsadresser.plugin.maskinporten.protectWithScopes

class VerifyScopeTest : WordSpec({
    val scope = Scope("test-scope")
    val organisasjonsnummer = Organisasjonsnummer("889640782")
    val client = specWideTestApplication {
        application {
            install(Authentication) {
                jwt {
                    verifier {
                        JWT.require(Algorithm.none()).build()
                    }

                    validate { jwtCredential ->
                        JWTPrincipal(jwtCredential.payload)
                    }
                }
            }

            routing {
                authenticate {
                    route("/test") {
                        protectWithScopes(setOf(scope))
                        protectWithOrganisasjonsnummer(setOf(organisasjonsnummer))
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

        "respond with FORBIDDEN when no scope is provided" {
            val response = client.get("/test/hello") {
                bearerAuth(JWT.create().sign(Algorithm.none()))
            }

            response.status.value shouldBe 403
        }

        "respond with FORBIDDEN when wrong scope is provided" {
            val response = client.get("/test/hello") {
                bearerAuth(
                    JWT.create()
                        .withClaim("scope", "wrong-scope")
                        .sign(Algorithm.none())
                )
            }

            response.status.value shouldBe 403
        }

        "respond with UNAUTHORIZED when no consumer.ID claim is provided" {
            val response = client.get("/test/hello") {
                bearerAuth(
                    JWT.create()
                        .withClaim("scope", scope.value)
                        .sign(Algorithm.none())
                )
            }

            response.status.value shouldBe 401
        }

        "respond with OK when correct jwt is provided" {
            val response = client.get("/test/hello") {
                bearerAuth(
                    JWT.create()
                        .withClaim("scope", scope.value)
                        .withClaim("consumer", mapOf("ID" to "0192:889640782"))
                        .sign(Algorithm.none())
                )
            }

            response.status.value shouldBe 200
        }
    }
})
