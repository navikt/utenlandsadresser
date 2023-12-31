package no.nav.utenlandsadresser.routes

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.utenlandsadresser.plugins.config.BasicAuthConfig
import no.nav.utenlandsadresser.plugins.configureBasicAuthDev

class DevRouteTest : WordSpec({
    "GET /dev/hello without configured credentials" should {
        "respond with UNAUTHORIZED" {
            testApplication {
                application {
                    configureBasicAuthDev(null)
                }
                routing {
                    configureDevRoutes(mockk())
                }

                val result = client.get("/dev/hello")

                result.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    "GET /dev/hello with configured credentials" should {
        val applicationConfig: ApplicationTestBuilder.() -> Unit = {
            application {
                configureBasicAuthDev(BasicAuthConfig("user", "password").getOrNull())
            }
            routing {
                configureDevRoutes(mockk())
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
