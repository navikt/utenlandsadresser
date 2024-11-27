package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.app.SporingsloggRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import kotlin.time.Duration

@Serializable
data class SporingsloggJson(
    val identitetsnummer: Identitetsnummer,
    val organisasjonsnummer: Organisasjonsnummer,
    val dataTilLogging: JsonElement,
)

fun Route.configureSporingsloggRoutes(sporingsloggRepository: SporingsloggRepository) {
    route("/sporingslogg") {
        // Skriv sporingslogg
        post<SporingsloggJson> { json ->
            sporingsloggRepository.loggJson(
                identitetsnummer = json.identitetsnummer,
                organisasjonsnummer = json.organisasjonsnummer,
                json = json.dataTilLogging,
            )

            call.respond(HttpStatusCode.OK)
        }
        // Slett logger eldre enn request parameter
        delete {
            val duration =
                runCatching {
                    call.request.queryParameters["olderThan"]?.let {
                        Duration.parse(it)
                    } ?: throw IllegalArgumentException("Missing duration parameter")
                }.getOrElse {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        """Invalid or missing duration parameter. Provide query parameter "olderThan" in ISO-8601 duration format, e.g. "PT87600H" for 10 years.""",
                    )
                    return@delete
                }

            sporingsloggRepository.deleteSporingsloggerOlderThan(duration)
            call.respond(HttpStatusCode.OK)
        }
    }
}
