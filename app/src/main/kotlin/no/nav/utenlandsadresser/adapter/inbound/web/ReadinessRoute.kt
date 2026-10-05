package no.nav.utenlandsadresser.adapter.inbound.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

// Readiness probe
fun Route.configureReadinessRoute() {
    get("/isready") {
        call.respond(HttpStatusCode.OK)
    }
}
