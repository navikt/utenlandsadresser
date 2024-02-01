package no.nav.utenlandsadresser.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Scope

class VerifyScopeTest : WordSpec({
    val scope = Scope("test-scope")
    val clientId = ClientId("test-client-id")
    val client = specWideTestApplication {
        application {
            routing {
                route("/test") {
                    install(VerifyScopeFromJwt) {
                        this.scope = scope
                    }
                    get("/hello") {
                        // Verify the client id is assigned to call attributes
                        call.attributes[ClientIdKey] shouldBe clientId.value

                        call.respond("Hello")
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

        "respond with UNAUTHORIZED when no client id is provided" {
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
                        .withClaim("client_id", clientId.value)
                        .sign(Algorithm.none())
                )
            }

            response.status.value shouldBe 200
        }
    }
})