package no.nav.utenlandsadresser.routes

import arrow.core.getOrElse
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
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Fødselsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.plugins.configureSerialization

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
        .withClaim("client_id", "test-client-id")
        .withClaim("scope", scope.value)
        .sign(Algorithm.none())

    val validFødselsnummer = Fødselsnummer("12345678910")
        .getOrElse { fail("Invalid fødselsnummer") }
    val invalidFødselsnummer = "123456789"

    "POST /postadresse/abonnement/start" should {
        "return 401 if jwt is missing" {
            val response = client.post("/postadresse/abonnement/start") {
                // language=json
                setBody("""{"fnr": "${validFødselsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

        "return 400 if fnr is invalid" {
            val response = client.post("/postadresse/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"fnr": "$invalidFødselsnummer"}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }

        "return 201 if fnr is valid" {
            every { abonnementService.startAbonnement(any(), any()) } returns Abonnement(
                clientId = ClientId("test-client-id"),
                fødselsnummer = validFødselsnummer,
                løpenummer = 0,
                opprettet = Clock.System.now(),
            )
            val response = client.post("/postadresse/abonnement/start") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"fnr": "${validFødselsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Created
        }
    }

    "POST /postadresse/abonnement/stopp" should {
        "return 401 if jwt is missing" {
            val response = client.post("/postadresse/abonnement/stopp") {
                // language=json
                setBody("""{"fnr": "${validFødselsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.Unauthorized
        }

        "return 400 if fnr is invalid" {
            val response = client.post("/postadresse/abonnement/stopp") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"fnr": "$invalidFødselsnummer"}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }

        "return 204 if fnr is valid" {
            every { abonnementService.stoppAbonnement(any(), any()) } returns Unit
            val response = client.post("/postadresse/abonnement/stopp") {
                bearerAuth(jwt)
                contentType(ContentType.Application.Json)
                // language=json
                setBody("""{"fnr": "${validFødselsnummer.value}"}""")
            }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }
})