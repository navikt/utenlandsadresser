package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.left
import arrow.core.right
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.app.*
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.plugin.configureSerialization
import java.util.*

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
    val feedEvent = FeedEvent.Outgoing(
        identitetsnummer = validIdentitetsnummer,
        abonnementId = UUID.randomUUID()
    )

    val abonnement = Abonnement(
        id = UUID.randomUUID(),
        identitetsnummer = validIdentitetsnummer,
        organisasjonsnummer = Organisasjonsnummer("889640782"),
        opprettet = Clock.System.now()
    )

    val basePath = "api/v1/postadresse"

    "POST /postadresse/abonnement/start" should {
        "return 401 when jwt is missing" {
            val response = client.post("$basePath/abonnement/start") {
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

        "return 200 when abonnement already exists" {
            coEvery {
                abonnementService.startAbonnement(
                    any(),
                    any()
                )
            } returns StartAbonnementError.AbonnementAlreadyExists(abonnement).left()
            val response = client.post("$basePath/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.OK
            // language=json
            response.bodyAsText() shouldEqualJson """
                {
                  "abonnementId": "${abonnement.id}"
                }
            """.trimIndent()
        }

        "return 415 when content type header is missing" {
            val response = client.post("$basePath/abonnement/start") {
                bearerAuth(jwt)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.UnsupportedMediaType
        }

        "return 201 when abonnement is started" {
            coEvery { abonnementService.startAbonnement(any(), any()) } returns abonnement.right()
            val response = client.post("$basePath/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Created
            // language=json
            response.bodyAsText() shouldEqualJson """
                {
                  "abonnementId": "${abonnement.id}"
                }
            """.trimIndent()
        }
    }

    "POST /postadresse/abonnement/stopp" should {
        "return 401 when jwt is missing" {
            val response = client.post("$basePath/abonnement/stopp") {
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

        "return 415 when content type header is missing" {
            val response = client.post("$basePath/abonnement/stopp") {
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
            val response = client.post("$basePath/abonnement/stopp") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"abonnementId": "${abonnement.id}"}""")
            }

            response.status shouldBe HttpStatusCode.OK
        }

        "return 200 when abonnement is stopped" {
            coEvery { abonnementService.stopAbonnement(any(), any()) } returns Unit.right()
            val response = client.post("$basePath/abonnement/stopp") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"abonnementId": "${abonnement.id}"}""")
            }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    "POST $basePath/feed" should {
        "return 401 when jwt is missing" {
            val response = client.post("$basePath/feed") {
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

        "return 400 when json is incorrect" {
            val response = client.post("$basePath/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"feil": "1"}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }

        "return 415 when content type header is missing" {
            val response = client.post("$basePath/feed") {
                bearerAuth(jwt)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.UnsupportedMediaType
        }

        "return 500 when feedService fails to get postadresse" {
            coEvery { feedService.readNext(any(), any()) } returns ReadFeedError.FailedToGetPostadresse.left()
            val response = client.post("$basePath/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.InternalServerError
        }

        "return 204 when feedService returns feed event not found" {
            coEvery { feedService.readNext(any(), any()) } returns ReadFeedError.FeedEventNotFound.left()
            val response = client.post("$basePath/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.NoContent
        }

        "return 200 and empty postadresse when postadresse is not found" {
            coEvery { feedService.readNext(any(), any()) } returns (feedEvent to Postadresse.Empty).right()
            val response = client.post("$basePath/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.OK
            // language=json
            response.bodyAsText() shouldEqualJson """
                {
                  "abonnementId": "${feedEvent.abonnementId}",
                  "identitetsnummer": "12345678910",
                  "utenlandskPostadresse": {
                    "adresselinje1": null,
                    "adresselinje2": null,
                    "adresselinje3": null,
                    "postnummer": null,
                    "poststed": null,
                    "landkode": null,
                    "land": null
                  }
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
            coEvery { feedService.readNext(any(), any()) } returns (feedEvent to postadresse).right()
            val response = client.post("$basePath/feed") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"løpenummer": "1"}""")
            }

            response.status shouldBe HttpStatusCode.OK
            // language=json
            response.bodyAsText() shouldEqualJson """
                {
                  "abonnementId": "${feedEvent.abonnementId}",
                  "identitetsnummer": "12345678910",
                  "utenlandskPostadresse": {
                    "adresselinje1": null,
                    "adresselinje2": null,
                    "adresselinje3": null,
                    "postnummer": null,
                    "poststed": null,
                    "landkode": "SE",
                    "land": "Sverige"
                  }
                }
            """.trimIndent()
        }
    }
})