package no.nav.utenlandsadresser.adapter.inbound.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.utenlandsadresser.application.port.outbound.SporingsloggRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.felles.sporingslogg.SporingsloggJson
import kotlin.time.Clock
import kotlin.time.Duration

fun Route.configureSporingsloggRoutes(
    sporingsloggRepository: SporingsloggRepository,
    clock: Clock,
) {
    route("/sporingslogg") {
        // Skriv sporingslogg
        post<SporingsloggJson> { json ->
            sporingsloggRepository.loggJson(
                identitetsnummer = Identitetsnummer(json.identitetsnummer),
                organisasjonsnummer = Organisasjonsnummer(json.organisasjonsnummer),
                json = json.dataTilLogging,
                tidspunktForUtlevering = clock.now(),
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

            sporingsloggRepository.slettSporingsloggerEldreEnn(duration)
            call.respond(HttpStatusCode.OK)
        }
    }
}
