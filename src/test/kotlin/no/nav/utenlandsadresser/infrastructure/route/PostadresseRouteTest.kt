package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.fail
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.app.*
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.plugin.configureSerialization

class PostadresseRouteTest : WordSpec({
    val abonnementService = mockk<AbonnementService>()
    val feedService = mockk<FeedService>()
    val scope = Scope("postadresse")
    val client = specWideTestApplication {
        application {
            configureSerialization()
            routing {
                configurePostadresseRoutes(
                    scope = scope,
                    abonnementService = abonnementService,
                    feedService = feedService,
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

        "return 204 when abonnement already exists" {
            coEvery {
                abonnementService.startAbonnement(
                    any(),
                    any()
                )
            } returns StartAbonnementError.AbonnementAlreadyExists.left()
            val response = client.post("/postadresse/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.NoContent
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
            coEvery { abonnementService.startAbonnement(any(), any()) } returns Unit.right()
            val response = client.post("/postadresse/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Created
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

        "return 200 when abonnement was already deleted or did not exist" {
            coEvery {
                abonnementService.stopAbonnement(any(), any())
            } returns StoppAbonnementError.AbonnementNotFound.left()
            val response = client.post("/postadresse/abonnement/stopp") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.OK
        }

        "return 200 when abonnement is stopped" {
            coEvery { abonnementService.stopAbonnement(any(), any()) } returns Unit.right()
            val response = client.post("/postadresse/abonnement/stopp") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    "POST /postadresse/feed" should {
        "return 401 when jwt is missing" {
            val response = client.post("/postadresse/feed") {
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

        "return 400 when json is incorrect" {
            val response = client.post("/postadresse/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"feil": "1"}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }

        "return 415 when content type header is missing" {
            val response = client.post("/postadresse/feed") {
                bearerAuth(jwt)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.UnsupportedMediaType
        }

        "return 500 when feedService fails to get postadresse" {
            coEvery { feedService.readFeed(any(), any()) } returns ReadFeedError.FailedToGetPostadresse.left()
            val response = client.post("/postadresse/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.InternalServerError
        }

        "return 204 when feedService returns feed event not found" {
            coEvery { feedService.readFeed(any(), any()) } returns ReadFeedError.FeedEventNotFound.left()
            val response = client.post("/postadresse/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.NoContent
        }

        "return 200 and empty postadresse when postadresse is not found" {
            coEvery { feedService.readFeed(any(), any()) } returns ReadFeedError.PostadresseNotFound.left()
            val response = client.post("/postadresse/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldEqualJson """
                {
                    "adresselinje1": null,
                    "adresselinje2": null,
                    "adresselinje3": null,
                    "postnummer": null,
                    "poststed": null,
                    "landkode": null,
                    "land": null
                }
            """.trimIndent()
        }

        "return 200 and postadresse when feedService returns postadresse" {
            val postadresse = Postadresse.Utenlandsk(
                adresselinje1 = null,
                adresselinje2 = null,
                adresselinje3 = null,
                postnummer = null,
                poststed = null,
                landkode = Landkode(value = "SE"),
                land = Land(value = "Sverige")

            )
            coEvery { feedService.readFeed(any(), any()) } returns postadresse.right()
            val response = client.post("/postadresse/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldEqualJson """
                {
                    "adresselinje1": null,
                    "adresselinje2": null,
                    "adresselinje3": null,
                    "postnummer": null,
                    "poststed": null,
                    "landkode": "SE",
                    "land": "Sverige"
                }
            """.trimIndent()
        }
    }
})