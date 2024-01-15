package no.nav.utenlandsadresser.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.configureDevRoutes() {
    route("/dev") {
        authenticate("basic-dev-auth", "form-dev-auth") {
            get("/hello") {
                call.respond(HttpStatusCode.OK, "Hello, world!")
            }
        }
    }
}
