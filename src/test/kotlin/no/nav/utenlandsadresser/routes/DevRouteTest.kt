package no.nav.utenlandsadresser.routes

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.utenlandsadresser.plugins.security.DevApiCredentials
import no.nav.utenlandsadresser.plugins.security.configureSecurity

class DevRouteTest : WordSpec({
    "GET /dev/hello without configured credentials" should {
        "respond with UNAUTHORIZED" {
            testApplication {
                application {
                    configureSecurity(null)
                }
                routing {
                    configureDevRoutes()
                }

                val result = client.get("/dev/hello")

                result.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    "GET /dev/hello with configured credentials" should {
        val applicationConfig: ApplicationTestBuilder.() -> Unit = {
            application {
                configureSecurity(DevApiCredentials("user", "password").getOrNull())
            }
            routing {
                configureDevRoutes()
            }
        }

        "respond with UNAUTHORIZED when credentials are not provided in request" {
            testApplication {
                applicationConfig()

                val result = client.get("/dev/hello")
                result.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "respond with UNAUTHORIZED when provided credentials are wrong" {
            testApplication {
                applicationConfig()
                val result = client.get("/dev/hello") {
                    basicAuth("user", "wrong-password")
                }
                result.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "respond with OK when credentials are correct" {
            testApplication {
                applicationConfig()
                val result = client.get("/dev/hello") {
                    basicAuth("user", "password")
                }
                result.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
