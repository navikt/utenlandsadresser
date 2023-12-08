package no.nav.utenlandsadresser.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.routes.configureLivenessRoute
import no.nav.utenlandsadresser.routes.configureReadinessRoute

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("""
                Puffet ris danser,
                Sprøtt i munnens glede, lett,
                Smaken av luftig.
            """.trimIndent())
        }
        configureLivenessRoute()
        configureReadinessRoute()
    }
}
