package no.nav.utenlandsadresser.routes

import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Fødselsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.plugins.ClientIdKey
import no.nav.utenlandsadresser.plugins.VerifyScopeFromJwt

fun Route.configurePostadresseRoutes(
    scope: Scope,
    abonnementService: AbonnementService,
) {
    route("/postadresse") {
        install(VerifyScopeFromJwt) {
            this.scope = scope
        }
        route("/abonnement") {
            post<StartAbonnementJson>("/start") { json ->
                val clientId = ClientId(call.attributes[ClientIdKey])
                val fødselsnummer = Fødselsnummer(json.fnr)
                    .getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, "Invalid fødselsnummer")
                    }

                abonnementService.startAbonnement(fødselsnummer, clientId)

                call.respond(HttpStatusCode.Created)
            }

            post<StoppAbonnementJson>("/stopp") { json ->
                val clientId = ClientId(call.attributes[ClientIdKey])
                val fødselsnummer = Fødselsnummer(json.fnr)
                    .getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, "Invalid fødselsnummer")
                    }

                abonnementService.stoppAbonnement(fødselsnummer, clientId)

                call.respond(HttpStatusCode.NoContent)
            }
        }
        post("/feed") {
            return@post call.respond(HttpStatusCode.NotImplemented, "Not implemented yet")
        }
    }
}

@Serializable
data class StartAbonnementJson(
    val fnr: String,
)

@Serializable
data class StoppAbonnementJson(
    val fnr: String,
)
