package no.nav.utenlandsadresser.adapter.inbound.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.utenlandsadresser.adapter.health.HealthCheck
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("LivenessRoute")

// Liveness probe
fun Route.configureLivenessRoute(healthChecks: List<HealthCheck>) {
    get("/isalive") {
        healthChecks.forEach { healthCheck ->
            if (!healthCheck.isHealthy()) {
                logger.error("Liveness check failed for ${healthCheck::class.simpleName}")
                call.respond(HttpStatusCode.InternalServerError)
                return@get
            }
        }
        call.respond(HttpStatusCode.OK)
    }
}
