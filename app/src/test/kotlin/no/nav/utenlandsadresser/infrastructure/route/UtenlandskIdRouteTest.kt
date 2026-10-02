package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.left
import arrow.core.right
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeatureToggles
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.app.NoopMetrikker
import no.nav.utenlandsadresser.app.ReadUtenlandskIdFeedError
import no.nav.utenlandsadresser.app.StartUtenlandskIdAbonnementError
import no.nav.utenlandsadresser.app.StoppAbonnementError
import no.nav.utenlandsadresser.app.Toggle
import no.nav.utenlandsadresser.app.UtenlandskIdAbonnementService
import no.nav.utenlandsadresser.app.UtenlandskIdFeedService
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Iso3166Alpha3
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetKilde
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetsnummer
import no.nav.utenlandsadresser.kotest.extension.specWideTestApplication
import no.nav.utenlandsadresser.plugin.configureSerialization
import no.nav.utenlandsadresser.plugin.maskinporten.configureMaskinportenAuthentication
import no.nav.utenlandsadresser.plugin.maskinporten.validateOrganisasjonsnummer
import kotlin.time.Clock
import kotlin.uuid.Uuid

class UtenlandskIdRouteTest :
    WordSpec({
        val abonnementService = mockk<UtenlandskIdAbonnementService>()
        val feedService = mockk<UtenlandskIdFeedService>()
        var utenlandskIdEnabled = true
        val featureToggles =
            object : FeatureToggles {
                override fun isEnabled(toggle: Toggle): Boolean = toggle == Toggle.UTENLANDSK_ID && utenlandskIdEnabled
            }

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

        val utenlandskIdToken = token(utenlandskIdScope)
        val postadresseToken = token(postadresseScope)

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
                        configureUtenlandskIdRoutes(abonnementService, feedService, NoopMetrikker, featureToggles)
                        configurePostadresseRoutes(mockk<AbonnementService>(), mockk<FeedService>(), NoopMetrikker)
                    }
                }
            }.client

        val identitetsnummer = Identitetsnummer("12345678910")
        val abonnement = Abonnement(Uuid.random(), organisasjonsnummer, identitetsnummer, Clock.System.now())
        val feedEvent =
            UtenlandskIdFeedEvent.Outgoing(
                identitetsnummer = identitetsnummer,
                abonnementId = abonnement.id,
                hendelsestype = UtenlandskIdHendelsestype.OppdatertUtenlandskId,
            )
        val utenlandskIdentitet =
            UtenlandskIdentitet(
                identitetsnummer = UtenlandskIdentitetsnummer("123010190B456"),
                utstederland = Iso3166Alpha3("DEU"),
                kilde = UtenlandskIdentitetKilde("Dolly"),
            )

        val basePath = "/api/v1/utenlandskid"

        beforeTest {
            clearAllMocks()
            utenlandskIdEnabled = true
        }

        "feature toggle" should {
            "return 404 on all endpoints when the toggle is off" {
                utenlandskIdEnabled = false

                listOf(
                    "$basePath/abonnement/start" to """{"identitetsnummer":"${identitetsnummer.value}"}""",
                    "$basePath/abonnement/stopp" to """{"abonnementId":"${abonnement.id}"}""",
                    "$basePath/feed" to """{"løpenummer":"0"}""",
                ).forEach { (path, body) ->
                    val response =
                        client.post(path) {
                            bearerAuth(utenlandskIdToken)
                            contentType(ContentType.Application.Json)
                            setBody(body)
                        }

                    response.status shouldBe HttpStatusCode.NotFound
                }
                coVerify(exactly = 0) { abonnementService.startAbonnement(any(), any()) }
                coVerify(exactly = 0) { abonnementService.stopAbonnement(any(), any()) }
                coVerify(exactly = 0) {
                    context(NoopMetrikker) { feedService.readNext(any(), any()) }
                }
            }

            "return 404 instead of 401 without token when the toggle is off" {
                utenlandskIdEnabled = false

                val response =
                    client.post("$basePath/feed") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"løpenummer":"0"}""")
                    }

                response.status shouldBe HttpStatusCode.NotFound
            }

            "not affect the postadresse api when the toggle is off" {
                utenlandskIdEnabled = false

                val response =
                    client.post("/api/v1/postadresse/feed") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"løpenummer":"0"}""")
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "tilgangskontroll" should {
            "reject a token with only the postadresse scope" {
                val response =
                    client.post("$basePath/abonnement/start") {
                        bearerAuth(postadresseToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"identitetsnummer":"${identitetsnummer.value}"}""")
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }

            "reject a token with only the utenlandsk id scope on the postadresse api" {
                val response =
                    client.post("/api/v1/postadresse/feed") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"løpenummer":"0"}""")
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }

            "reject a request without token" {
                val response =
                    client.post("$basePath/feed") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"løpenummer":"0"}""")
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "POST /abonnement/start" should {
            "return 201 when the abonnement is created" {
                coEvery { abonnementService.startAbonnement(identitetsnummer, organisasjonsnummer) } returns abonnement.right()

                val response =
                    client.post("$basePath/abonnement/start") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"identitetsnummer":"${identitetsnummer.value}"}""")
                    }

                response.status shouldBe HttpStatusCode.Created
                response.bodyAsText() shouldEqualJson """{"abonnementId":"${abonnement.id}"}"""
            }

            "return 200 with the existing abonnement when it already exists" {
                coEvery { abonnementService.startAbonnement(any(), any()) } returns
                    StartUtenlandskIdAbonnementError.AbonnementAlreadyExists(abonnement).left()

                val response =
                    client.post("$basePath/abonnement/start") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"identitetsnummer":"${identitetsnummer.value}"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldEqualJson """{"abonnementId":"${abonnement.id}"}"""
            }

            "return 500 when the lookup fails" {
                coEvery { abonnementService.startAbonnement(any(), any()) } returns
                    StartUtenlandskIdAbonnementError.FailedToGetUtenlandskId.left()

                val response =
                    client.post("$basePath/abonnement/start") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"identitetsnummer":"${identitetsnummer.value}"}""")
                    }

                response.status shouldBe HttpStatusCode.InternalServerError
            }
        }

        "POST /abonnement/stopp" should {
            "return 200 when the abonnement is stopped" {
                coEvery { abonnementService.stopAbonnement(abonnement.id, organisasjonsnummer) } returns Unit.right()

                val response =
                    client.post("$basePath/abonnement/stopp") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"abonnementId":"${abonnement.id}"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
            }

            "return 200 when the abonnement does not exist" {
                coEvery { abonnementService.stopAbonnement(any(), any()) } returns StoppAbonnementError.AbonnementNotFound.left()

                val response =
                    client.post("$basePath/abonnement/stopp") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"abonnementId":"${abonnement.id}"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }

        "POST /feed" should {
            "return the utenlandske id-er for the next event" {
                coEvery {
                    context(NoopMetrikker) { feedService.readNext(Løpenummer(0), organisasjonsnummer) }
                } returns
                    (feedEvent to listOf(utenlandskIdentitet)).right()

                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"løpenummer":"0"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldEqualJson
                    """
                    {
                      "abonnementId": "${abonnement.id}",
                      "identitetsnummer": "${identitetsnummer.value}",
                      "hendelsestype": "OPPDATERT_UTENLANDSK_ID",
                      "utenlandskId": [
                        {"identitetsnummer": "123010190B456", "utstederland": "DEU", "kilde": "Dolly"}
                      ]
                    }
                    """.trimIndent()
            }

            "return the event with an empty list when the person no longer has utenlandsk id" {
                coEvery {
                    context(NoopMetrikker) { feedService.readNext(any(), any()) }
                } returns (feedEvent to emptyList<UtenlandskIdentitet>()).right()

                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"løpenummer":"0"}""")
                    }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldEqualJson
                    """
                    {
                      "abonnementId": "${abonnement.id}",
                      "identitetsnummer": "${identitetsnummer.value}",
                      "hendelsestype": "OPPDATERT_UTENLANDSK_ID",
                      "utenlandskId": []
                    }
                    """.trimIndent()
            }

            "return 204 when there is no event on the next løpenummer" {
                coEvery {
                    context(NoopMetrikker) { feedService.readNext(any(), any()) }
                } returns ReadUtenlandskIdFeedError.FeedEventNotFound.left()

                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"løpenummer":"0"}""")
                    }

                response.status shouldBe HttpStatusCode.NoContent
            }

            "return 500 when the lookup fails" {
                coEvery {
                    context(NoopMetrikker) { feedService.readNext(any(), any()) }
                } returns ReadUtenlandskIdFeedError.FailedToGetUtenlandskId.left()

                val response =
                    client.post("$basePath/feed") {
                        bearerAuth(utenlandskIdToken)
                        contentType(ContentType.Application.Json)
                        setBody("""{"løpenummer":"0"}""")
                    }

                response.status shouldBe HttpStatusCode.InternalServerError
            }
        }
    })
