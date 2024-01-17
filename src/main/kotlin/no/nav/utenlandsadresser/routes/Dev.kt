package no.nav.utenlandsadresser.routes

import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.clients.RegOppslagClient
import no.nav.utenlandsadresser.domain.Fødselsnummer
import no.nav.utenlandsadresser.domain.Postadresse

fun Route.configureDevRoutes(
    regOppslagClient: RegOppslagClient,
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

            val postAdresse = regOppslagClient.getPostadresse(fødselsnummer)
                .getOrElse {
                    return@post when (it) {
                        RegOppslagClient.Error.IngenTilgang -> call.respond(
                            HttpStatusCode.InternalServerError,
                            "Ingen tilgang"
                        )

                        is RegOppslagClient.Error.Ukjent -> call.respond(
                            HttpStatusCode.InternalServerError,
                            it.message
                        )

                        RegOppslagClient.Error.UgyldigForespørsel -> call.respond(
                            HttpStatusCode.InternalServerError,
                            "Ugyldig forespørsel"
                        )

                        RegOppslagClient.Error.UkjentAdresse -> call.respond(
                            HttpStatusCode.InternalServerError,
                            "Ukjent adresse"
                        )
                    }
                }

            return@post call.respond(HttpStatusCode.OK, PostadresseResponseJson.fromDomain(postAdresse))
        }
    }
}

@Serializable
data class PostadresseResponseJson(
    val type: String,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
    val land: String,
) {
    companion object {
        fun fromDomain(postadresse: Postadresse): PostadresseResponseJson = when (postadresse) {
            is Postadresse.Utenlandsk -> PostadresseResponseJson(
                type = "UTENLANDSK",
                adresselinje1 = postadresse.adresselinje1?.value,
                adresselinje2 = postadresse.adresselinje2?.value,
                adresselinje3 = postadresse.adresselinje3?.value,
                postnummer = postadresse.postnummer?.value,
                poststed = postadresse.poststed?.value,
                landkode = postadresse.landkode.value,
                land = postadresse.land.value,
            )

            is Postadresse.Norsk -> PostadresseResponseJson(
                type = "NORSK",
                adresselinje1 = postadresse.adresselinje1?.value,
                adresselinje2 = postadresse.adresselinje2?.value,
                adresselinje3 = postadresse.adresselinje3?.value,
                postnummer = postadresse.postnummer?.value,
                poststed = postadresse.poststed?.value,
                landkode = postadresse.landkode.value,
                land = postadresse.land.value,
            )
        }
    }
}

@Serializable
data class RegOppslagRequest(
    val fnr: String,
)
