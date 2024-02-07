package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.plugin.OrganisasjonsnummerKey
import no.nav.utenlandsadresser.plugin.VerifyScopeFromJwt

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
                val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                val identitetsnummer = Identitetsnummer(json.identitetsnummer)
                    .getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, "Invalid identitetsnummer")
                    }

                abonnementService.startAbonnement(identitetsnummer, organisasjonsnummer)

                call.respond(HttpStatusCode.Created)
            }

            post<StoppAbonnementJson>("/stopp") { json ->
                val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                val identitetsnummer = Identitetsnummer(json.identitetsnummer)
                    .getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, "Invalid identitetsnummer")
                    }

                abonnementService.stoppAbonnement(identitetsnummer, organisasjonsnummer)

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
    val identitetsnummer: String,
)

@Serializable
data class StoppAbonnementJson(
    val identitetsnummer: String,
)
