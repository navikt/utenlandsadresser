package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Liveness probe
fun Route.configureLivenessRoute() {
    get("/isalive") {
        call.respond(HttpStatusCode.OK)
    }
}