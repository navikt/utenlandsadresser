package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Readiness probe
fun Route.configureReadinessRoute() {
    get("/isready") {
        call.respond(HttpStatusCode.OK)
    }
}