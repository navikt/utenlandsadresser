package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.MaskinportenClient
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.route.json.PostadresseDevResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.RegOppslagRequest

fun Route.configureDevRoutes(
    registeroppslagClient: RegisteroppslagClient,
    maskinportenClient: MaskinportenClient,
) {
    route("/dev") {
        post("/regoppslag") {
            val request = call.receive<RegOppslagRequest>()
            val identitetsnummer = Identitetsnummer(request.fnr)

            val postAdresse =
                registeroppslagClient
                    .getPostadresse(identitetsnummer)
                    .getOrElse {
                        return@post when (it) {
                            GetPostadresseError.IngenTilgang ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Ingen tilgang",
                                )

                            is GetPostadresseError.UkjentFeil ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    it.message,
                                )

                            GetPostadresseError.UgyldigForespørsel ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Ugyldig forespørsel",
                                )

                            GetPostadresseError.UkjentAdresse ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Ukjent adresse",
                                )
                        }
                    }

            call.respond(HttpStatusCode.OK, PostadresseDevResponseJson.fromDomain(postAdresse))
        }
        get("/maskinporten/token") {
            val token = maskinportenClient.getAccessToken()
            call.respond(HttpStatusCode.OK, token)
        }
    }
}
