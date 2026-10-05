package no.nav.utenlandsadresser.adapter.inbound.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.utenlandsadresser.application.port.inbound.SkrivSporingslogg
import no.nav.utenlandsadresser.application.port.inbound.SlettSporingslogg
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.felles.sporingslogg.SporingsloggJson
import kotlin.time.Duration

fun Route.configureSporingsloggRoutes(
    skrivSporingslogg: SkrivSporingslogg,
    slettSporingslogg: SlettSporingslogg,
) {
    route("/sporingslogg") {
        // Skriv sporingslogg
        post<SporingsloggJson> { json ->
            skrivSporingslogg.skriv(
                identitetsnummer = Identitetsnummer(json.identitetsnummer),
                organisasjonsnummer = Organisasjonsnummer(json.organisasjonsnummer),
                json = json.dataTilLogging.toString(),
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

            slettSporingslogg.slettEldreEnn(duration)
            call.respond(HttpStatusCode.OK)
        }
    }
}
