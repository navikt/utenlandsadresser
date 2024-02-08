package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.fail
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.mockk.every
import io.mockk.mockk
import kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.plugin.configureSerialization

class PostadresseRouteTest : WordSpec({
    val abonnementService = mockk<AbonnementService>()
    val scope = Scope("postadresse")
    val client = specWideTestApplication {
        application {
            configureSerialization()
            routing {
                configurePostadresseRoutes(
                    scope = scope,
                    abonnementService = abonnementService,
                )
            }
        }
    }.client

    val jwt = JWT.create()
        .withClaim("consumer", mapOf("ID" to "0192:889640782"))
        .withClaim("scope", scope.value)
        .sign(Algorithm.none())

    val validIdentitetsnummer = Identitetsnummer("12345678910")
        .getOrElse { fail("Invalid fødselsnummer") }
    val invalidIdentitetsnummer = "123456789"

    "POST /postadresse/abonnement/start" should {
        "return 401 when jwt is missing" {
            val response = client.post("/postadresse/abonnement/start") {
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

        "return 400 when identitetsnummer is invalid" {
            val response = client.post("/postadresse/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "$invalidIdentitetsnummer"}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }

        "return 400 when abonnement already exists" {
            every {
                abonnementService.startAbonnement(
                    any(),
                    any()
                )
            } returns AbonnementService.StartAbonnementError.AbonnementAlreadyExists.left()
            val response = client.post("/postadresse/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }

        "return 415 when content type header is missing" {
            val response = client.post("/postadresse/abonnement/start") {
                bearerAuth(jwt)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.UnsupportedMediaType
        }

        "return 201 when abonnement is started" {
            every { abonnementService.startAbonnement(any(), any()) } returns Unit.right()
            val response = client.post("/postadresse/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    "POST /postadresse/abonnement/stopp" should {
        "return 401 when jwt is missing" {
            val response = client.post("/postadresse/abonnement/stopp") {
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

        "return 400 when identitetsnummer is invalid" {
            val response = client.post("/postadresse/abonnement/stopp") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "$invalidIdentitetsnummer"}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }

        "return 415 when content type header is missing" {
            val response = client.post("/postadresse/abonnement/stopp") {
                bearerAuth(jwt)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.UnsupportedMediaType
        }

        "return 204 when abonnement is stopped" {
            every { abonnementService.stopAbonnement(any(), any()) } returns Unit
            val response = client.post("/postadresse/abonnement/stopp") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.OK
        }
    }
})