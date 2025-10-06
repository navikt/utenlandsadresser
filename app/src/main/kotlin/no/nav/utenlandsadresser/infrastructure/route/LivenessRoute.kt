package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.slf4j.Logger

// Liveness probe
fun Route.configureLivenessRoute(
    logger: Logger,
    healthChecks: List<HealthCheck>,
) {
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

interface HealthCheck {
    fun isHealthy(): Boolean
}
