package no.nav.utenlandsadresser.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.plugins.VerifyScopeFromJwt

fun Route.configurePostadresseRoutes(scope: Scope) {
    route("/postadresse") {
        install(VerifyScopeFromJwt) {
            this.scope = scope
        }
        route("/abonnement") {
            post("/start") {
                return@post call.respond(HttpStatusCode.NotImplemented, "Not implemented yet")
            }
            post("/stopp") {
                return@post call.respond(HttpStatusCode.NotImplemented, "Not implemented yet")
            }
        }
        post("/feed") {
            return@post call.respond(HttpStatusCode.NotImplemented, "Not implemented yet")
        }
    }
}
