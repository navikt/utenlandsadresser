package no.nav.utenlandsadresser.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.configureDevRoutes() {
    authenticate("basic-dev-auth", "form-dev-auth") {
        route("/dev") {
            get("/hello") {
                call.respondText("Hello, world!")
            }
        }
    }
}
