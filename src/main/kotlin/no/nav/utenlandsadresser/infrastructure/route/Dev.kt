package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.infrastructure.client.MaskinportenClient
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.domain.Fødselsnummer
import no.nav.utenlandsadresser.infrastructure.route.json.PostadresseResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.RegOppslagRequest

fun Route.configureDevRoutes(
    registeroppslagClient: RegisteroppslagClient,
    maskinportenClient: MaskinportenClient,
) {
    route("/dev") {
        authenticate("basic-dev-auth", "form-dev-auth") {
            get("/hello") {
                call.respond(HttpStatusCode.OK, "Hello, world!")
            }
        }
        post("/regoppslag") {
            val request = call.receive<RegOppslagRequest>()
            val fødselsnummer = Fødselsnummer(request.fnr)
                .getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig fødselsnummer")
                }

            val postAdresse = registeroppslagClient.getPostadresse(fødselsnummer)
                .getOrElse {
                    return@post when (it) {
                        RegisteroppslagClient.Error.IngenTilgang -> call.respond(
                            HttpStatusCode.InternalServerError,
                            "Ingen tilgang"
                        )

                        is RegisteroppslagClient.Error.Ukjent -> call.respond(
                            HttpStatusCode.InternalServerError,
                            it.message
                        )

                        RegisteroppslagClient.Error.UgyldigForespørsel -> call.respond(
                            HttpStatusCode.InternalServerError,
                            "Ugyldig forespørsel"
                        )

                        RegisteroppslagClient.Error.UkjentAdresse -> call.respond(
                            HttpStatusCode.InternalServerError,
                            "Ukjent adresse"
                        )
                    }
                }

            call.respond(HttpStatusCode.OK, PostadresseResponseJson.fromDomain(postAdresse))
        }
        get("/maskinporten/token") {
            val token = maskinportenClient.getAccessToken()
            call.respond(HttpStatusCode.OK, token)
        }
    }
}


