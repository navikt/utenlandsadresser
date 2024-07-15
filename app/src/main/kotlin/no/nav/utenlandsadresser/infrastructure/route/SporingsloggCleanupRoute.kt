package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.SporingsloggPostgresRepository
import kotlin.time.Duration

fun Route.configureSporingsloggCleanupRoute(sporingsloggPostgresRepository: SporingsloggPostgresRepository) {
    // Delete logs older than request parameter
    delete("/sporingslogg") {
        val duration =
            runCatching {
                call.request.queryParameters["olderThan"]?.let {
                    Duration.parse(it)
                } ?: throw IllegalArgumentException("Missing duration parameter")
            }.getOrElse {
                call.respond(
                    HttpStatusCode.BadRequest,
                    """Invalid or missing duration parameter. Provide query parameter "olderThan" in ISO-8601 duration format, e.g. "P" for 10 years.""",
                )
                return@delete
            }

        sporingsloggPostgresRepository.deleteSporingsloggerOlderThan(duration)
        call.respond(HttpStatusCode.OK)
    }
}
