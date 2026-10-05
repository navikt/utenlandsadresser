package no.nav.utenlandsadresser.adapter.inbound.web

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.utenlandsadresser.adapter.inbound.web.json.HentUtenlandskIdDevRequestJson
import no.nav.utenlandsadresser.adapter.inbound.web.json.PostadresseDevResponseJson
import no.nav.utenlandsadresser.adapter.inbound.web.json.RegOppslagRequest
import no.nav.utenlandsadresser.adapter.inbound.web.json.UtenlandskIdentitetDevResponseJson
import no.nav.utenlandsadresser.adapter.outbound.maskinporten.MaskinportenClient
import no.nav.utenlandsadresser.application.port.outbound.HentPostadresseError
import no.nav.utenlandsadresser.application.port.outbound.PostadresseOppslag
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdOppslag
import no.nav.utenlandsadresser.domain.Identitetsnummer

fun Route.configureDevRoutes(
    postadresseOppslag: PostadresseOppslag,
    maskinportenClient: MaskinportenClient,
    utenlandskIdOppslag: UtenlandskIdOppslag,
) {
    route("/dev") {
        post("/regoppslag") {
            val request = call.receive<RegOppslagRequest>()
            val identitetsnummer = Identitetsnummer(request.fnr)

            val postAdresse =
                postadresseOppslag
                    .hentPostadresse(identitetsnummer)
                    .getOrElse {
                        return@post when (it) {
                            HentPostadresseError.IngenTilgang -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Ingen tilgang",
                                )
                            }

                            is HentPostadresseError.UkjentFeil -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    it.message,
                                )
                            }

                            HentPostadresseError.UgyldigForespørsel -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Ugyldig forespørsel",
                                )
                            }

                            HentPostadresseError.UkjentAdresse -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Ukjent adresse",
                                )
                            }

                            HentPostadresseError.FalskIdentiet -> {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Falsk identitet",
                                )
                            }
                        }
                    }

            call.respond(HttpStatusCode.OK, PostadresseDevResponseJson.fromDomain(postAdresse))
        }

        post("/pdl/utenlandsk-id") {
            val request = call.receive<HentUtenlandskIdDevRequestJson>()
            val identiteter =
                utenlandskIdOppslag
                    .hentUtenlandskIdentitet(Identitetsnummer(request.identitetsnummer))
                    .getOrElse {
                        return@post call.respond(
                            HttpStatusCode.BadGateway,
                            "Feil ved oppslag av utenlandsk identitet",
                        )
                    }

            call.respond(HttpStatusCode.OK, identiteter.map(UtenlandskIdentitetDevResponseJson::fromDomain))
        }

        get("/maskinporten/token") {
            val token = maskinportenClient.getAccessToken()
            call.respond(HttpStatusCode.OK, token)
        }
    }
}
