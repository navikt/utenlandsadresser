package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.left
import arrow.core.right
import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.app.ReadFeedError
import no.nav.utenlandsadresser.app.StartAbonnementError
import no.nav.utenlandsadresser.app.StoppAbonnementError
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Land
import no.nav.utenlandsadresser.domain.Landkode
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.plugin.configureOpenApi
import no.nav.utenlandsadresser.plugin.configureSerialization
import no.nav.utenlandsadresser.plugin.maskinporten.configureMaskinportenAuthentication
import no.nav.utenlandsadresser.plugin.maskinporten.validateOrganisasjonsnummer
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import kotlin.time.Clock
import kotlin.uuid.Uuid

class PostadresseRouteTest :
    WordSpec({
        val abonnementService = mockk<AbonnementService>()
        val feedService = mockk<FeedService>()

        val scope = Scope("postadresse")
        val jwkProvider = mockk<JwkProvider>()
        val issuer = Issuer("https://maskinporten.no")

        val keyPair =
            KeyPairGenerator
                .getInstance("RSA")
                .apply {
                    initialize(2048)
                }.genKeyPair()
        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey
        val jwt =
            JWT
                .create()
                .withIssuer(issuer.value)
                .withClaim("consumer", mapOf("ID" to "0192:889640782"))
                .withClaim("scope", scope.value)
                .sign(Algorithm.RSA256(publicKey, privateKey))

        val validIdentitetsnummer = Identitetsnummer("12345678910")
        val feedEvent =
            FeedEvent.Outgoing(
                identitetsnummer = validIdentitetsnummer,
                abonnementId = Uuid.random(),
                hendelsestype = Hendelsestype.OppdatertAdresse,
            )
        val organisasjonsnummer = Organisasjonsnummer("889640782")
        val abonnement =
            Abonnement(
                id = Uuid.random(),
                identitetsnummer = validIdentitetsnummer,
                organisasjonsnummer = organisasjonsnummer,
                opprettet = Clock.System.now(),
            )

        val basePath = "api/v1/postadresse"

        val client =
            specWideTestApplication {
                application {
                    configureSerialization()
                    configureMaskinportenAuthentication(
                        "postadresse-abonnement-maskinporten",
                        issuer,
                        setOf(scope),
                        jwkProvider,
                        jwtValidationBlock = validateOrganisasjonsnummer(listOf(organisasjonsnummer.value)),
                    )
                    routing {
                        configurePostadresseRoutes(
                            abonnementService = abonnementService,
                            feedService = feedService,
                        )
                        configureOpenApi()
                    }
                }
            }.client

        beforeTest {
            every { jwkProvider.get(any()) } returns
                Jwk(
                    "keyId",
                    "RSA",
                    "RS256",
                    "",
                    emptyList(),
                    "",
                    emptyList(),
                    "",
                    mapOf(
                        "n" to Base64.getUrlEncoder().encodeToString(publicKey.modulus.toByteArray()),
                        "e" to Base64.getUrlEncoder().encodeToString(publicKey.publicExponent.toByteArray()),
                    ),
                )
        }

        "GET /docs/swagger/api.json" should {
            "preserve route summaries, descriptions and response documentation" {
                val response = client.get("/docs/swagger/api.json")
                response.status shouldBe HttpStatusCode.OK
                val paths = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("paths").jsonObject
                val expectedOperations = mapOf(
                    "/abonnement/start" to Triple(
                        "Start abonnement",
                        "Start abonnement for en person med et gitt identitetsnummer. Om personen har en utenlandsk adresse ved start av abonnementet, vil denne adressen bli lagt på feeden. Eventuelle split og merge i Folkeregisteret på brukere som det er satt opp abonnement på må håndteres av Skatteetaten ved at man avslutter gjeldende abonnement og oppretter nytt abonnement.",
                        mapOf(
                            "200" to "Abonnementet på gitt identitetsnummer eksisterer allerede.",
                            "201" to "Abonnementet er opprettet.",
                            "400" to "Feil i forespørsel.",
                            "401" to "Manglende eller ugyldig Maskinporten-token.",
                            "500" to "Intern feil, abonnement ble ikke opprettet.",
                        ),
                    ),
                    "/abonnement/stopp" to Triple(
                        "Stopp abonnement",
                        "Stopp abonnement med en gitt referanse.",
                        mapOf(
                            "200" to "Abonnementet ble stoppet eller finnes ikke.",
                            "401" to "Manglende eller ugyldig Maskinporten-token.",
                        ),
                    ),
                    "/feed" to Triple(
                        "Hent neste postadresse",
                        "Hent neste postadresse fra feeden. Returnerer en utenlandsk postadresse om det finnes en. Vi skiller mellom to typer hendelser. OPPDATERT_ADRESSE betyr at det har skjedd en endring på en persons adresse. Responsen vil inneholde nåværende adresse. SLETTET_ADRESSE betyr at en persons adresse er slettet. Dette skjer i utgangspunktet ved adressebeskyttelse. Om man leser en event med denne hendelsestypen så forventes det at konsumenten sletter postadressen til personen.",
                        mapOf(
                            "200" to "",
                            "204" to "Ingen feed event på gitt løpenummer.",
                            "401" to "Manglende eller ugyldig Maskinporten-token.",
                            "500" to "Intern feil ved henting av postadresse.",
                        ),
                    ),
                )
                for ((path, expected) in expectedOperations) {
                    val operation = paths.getValue("/$basePath$path").jsonObject.getValue("post").jsonObject
                    operation.getValue("summary").jsonPrimitive.content shouldBe expected.first
                    operation["description"]?.jsonPrimitive?.content shouldBe expected.second
                    operation.getValue("responses").jsonObject.mapValues { (_, response) ->
                        response.jsonObject.getValue("description").jsonPrimitive.content
                    } shouldBe expected.third
                }
            }

            "document examples on request and response media types instead of schemas" {
                val response = client.get("/docs/swagger/api.json")
                response.status shouldBe HttpStatusCode.OK
                val document = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val paths = document.getValue("paths").jsonObject
                val requests = listOf(
                    Triple("/abonnement/start", "StartAbonnementRequestJson", """{"identitetsnummer":"12345678901"}"""),
                    Triple("/abonnement/stopp", "StoppAbonnementJson", """{"abonnementId":"f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f"}"""),
                    Triple("/feed", "FeedRequestJson", """{"løpenummer":"1"}"""),
                )
                for ((path, schema, expected) in requests) {
                    val request = paths.getValue("/$basePath$path").jsonObject.getValue("post").jsonObject
                        .getValue("requestBody").jsonObject.getValue("content").jsonObject
                        .getValue("application/json").jsonObject
                    request.getValue("schema").jsonObject.getValue("\$ref").jsonPrimitive.content shouldBe
                        "#/components/schemas/$schema"
                    request.getValue("examples").jsonObject.values.single().jsonObject
                        .getValue("value").toString() shouldEqualJson expected
                }

                val startResponses = paths.getValue("/$basePath/abonnement/start").jsonObject
                    .getValue("post").jsonObject.getValue("responses").jsonObject
                startResponses.keys shouldBe setOf("200", "201", "400", "401", "500")
                for (status in listOf("200", "201")) {
                    val content = startResponses.getValue(status).jsonObject.getValue("content").jsonObject
                        .getValue("application/json").jsonObject
                    content.getValue("schema").jsonObject.getValue("\$ref").jsonPrimitive.content shouldBe
                        "#/components/schemas/StartAbonnementResponseJson"
                    content.getValue("examples").jsonObject.getValue("abonnement").jsonObject
                        .getValue("value").toString() shouldEqualJson
                        """{"abonnementId":"f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f"}"""
                }

                val feedResponses = paths.getValue("/$basePath/feed").jsonObject.getValue("post").jsonObject
                    .getValue("responses").jsonObject
                feedResponses.keys shouldBe setOf("200", "204", "401", "500")
                val feedContent = feedResponses.getValue("200").jsonObject.getValue("content").jsonObject
                    .getValue("application/json").jsonObject
                feedContent.getValue("schema").jsonObject.getValue("\$ref").jsonPrimitive.content shouldBe
                    "#/components/schemas/PostadresseFeedResponseJson"
                val examples = feedContent.getValue("examples").jsonObject
                examples.keys shouldBe setOf("oppdatertAdresse", "ingenUtenlandskAdresse", "slettetAdresse")
                val values = examples.values.map { it.jsonObject.getValue("value").jsonObject }
                values.map { it.getValue("hendelsestype").jsonPrimitive.content } shouldBe
                    listOf("OPPDATERT_ADRESSE", "OPPDATERT_ADRESSE", "SLETTET_ADRESSE")
                values[0].getValue("utenlandskPostadresse").jsonObject.getValue("landkode").jsonPrimitive.content shouldBe "SE"
                values[1].getValue("utenlandskPostadresse") shouldBe JsonNull
                values[2].getValue("utenlandskPostadresse") shouldBe JsonNull

                val schemas = document.getValue("components").jsonObject.getValue("schemas").jsonObject
                for (schema in schemas.values) {
                    schema.jsonObject.containsKey("examples") shouldBe false
                }
            }
        }

        "POST /postadresse/abonnement/start" should {
            "return 401 when jwt is missing" {
                val response =
                    client.post("$basePath/abonnement/start") {
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
                        any(),
                    )
                } returns StartAbonnementError.AbonnementAlreadyExists(abonnement).left()
                val response =
                    client.post("$basePath/abonnement/start") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
                // language=json
                response.bodyAsText() shouldEqualJson
                    """
                    {
                      "abonnementId": "${abonnement.id}"
                    }
                    """.trimIndent()
            }

            "return 415 when content type header is missing" {
                val response =
                    client.post("$basePath/abonnement/start") {
                        bearerAuth(jwt)
                        // language=json
                        setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
                    }

                response.status shouldBe HttpStatusCode.UnsupportedMediaType
            }

            "return 201 when abonnement is started" {
                coEvery { abonnementService.startAbonnement(any(), any()) } returns abonnement.right()
                val response =
                    client.post("$basePath/abonnement/start") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
                    }

                response.status shouldBe HttpStatusCode.Created
                // language=json
                response.bodyAsText() shouldEqualJson
                    """
                    {
                      "abonnementId": "${abonnement.id}"
                    }
                    """.trimIndent()
            }
        }

        "POST /postadresse/abonnement/stopp" should {
            "return 401 when jwt is missing" {
                val response =
                    client.post("$basePath/abonnement/stopp") {
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"identitetsnummer": "${validIdentitetsnummer.value}"}""")
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }

            "return 415 when content type header is missing" {
                val response =
                    client.post("$basePath/abonnement/stopp") {
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
                val response =
                    client.post("$basePath/abonnement/stopp") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"abonnementId": "${abonnement.id}"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
            }

            "return 200 when abonnement is stopped" {
                coEvery { abonnementService.stopAbonnement(any(), any()) } returns Unit.right()
                val response =
                    client.post("$basePath/abonnement/stopp") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"abonnementId": "${abonnement.id}"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }

        "POST postadresse/feed" should {
            "return 401 when jwt is missing" {
                val response =
                    client.post("$basePath/feed") {
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"løpenummer": "1"}""")
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }

            "return 400 when json is incorrect" {
                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"feil": "1"}""")
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }

            "return 415 when content type header is missing" {
                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(jwt)
                        // language=json
                        setBody("""{"løpenummer": "1"}""")
                    }

                response.status shouldBe HttpStatusCode.UnsupportedMediaType
            }

            "return 500 when feedService fails to get postadresse" {
                coEvery { feedService.readNext(any(), any()) } returns ReadFeedError.FailedToGetPostadresse.left()
                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"løpenummer": "1"}""")
                    }

                response.status shouldBe HttpStatusCode.InternalServerError
            }

            "return 204 when feedService returns feed event not found" {
                coEvery { feedService.readNext(any(), any()) } returns ReadFeedError.FeedEventNotFound.left()
                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"løpenummer": "1"}""")
                    }

                response.status shouldBe HttpStatusCode.NoContent
            }

            "return 200 and empty postadresse when postadresse is not found" {
                coEvery { feedService.readNext(any(), any()) } returns (feedEvent to null).right()
                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"løpenummer": "1"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
                // language=json
                response.bodyAsText() shouldEqualJson
                    """
                    {
                      "abonnementId": "${feedEvent.abonnementId}",
                      "identitetsnummer": "12345678910",
                      "hendelsestype": "OPPDATERT_ADRESSE",
                      "utenlandskPostadresse": null
                    }
                    """.trimIndent()
            }

            "return 200 with postadresse when feedService returns postadresse" {
                val postadresse =
                    Postadresse.Utenlandsk(
                        adresselinje1 = null,
                        adresselinje2 = null,
                        adresselinje3 = null,
                        postnummer = null,
                        poststed = null,
                        landkode = Landkode(value = "SE"),
                        land = Land(value = "Sverige"),
                    )
                coEvery { feedService.readNext(any(), any()) } returns (feedEvent to postadresse).right()
                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"løpenummer": "1"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
                // language=json
                response.bodyAsText() shouldEqualJson
                    """
                    {
                      "abonnementId": "${feedEvent.abonnementId}",
                      "identitetsnummer": "12345678910",
                      "hendelsestype": "OPPDATERT_ADRESSE",
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

            "return 200 with delete postadresse event" {
                val deleteFeedEvent =
                    FeedEvent.Outgoing(
                        identitetsnummer = validIdentitetsnummer,
                        abonnementId = Uuid.random(),
                        hendelsestype = Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT),
                    )
                coEvery { feedService.readNext(any(), any()) } returns (deleteFeedEvent to null).right()

                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(jwt)
                        contentType(ContentType.Application.Json)
                        // language=json
                        setBody("""{"løpenummer": "1"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
                // language=json
                response.bodyAsText() shouldEqualJson
                    """
                    {
                      "abonnementId": "${deleteFeedEvent.abonnementId}",
                      "identitetsnummer": "12345678910",
                      "hendelsestype": "SLETTET_ADRESSE",
                      "utenlandskPostadresse": null
                    }
                    """.trimIndent()
            }
        }
    })
