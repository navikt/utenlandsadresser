package no.nav.utenlandsadresser.hent.utenlandsadresser.client

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.hent.utenlandsadresser.Sporingslogg
import no.nav.utenlandsadresser.infrastructure.route.json.SporingsloggJson
import java.net.URL

class UtenlandsadresserHttpClient(
    val httpClient: HttpClient,
    val baseUrl: URL,
) : Sporingslogg {
    override suspend fun logg(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        dataTilLogging: JsonElement,
    ) {
        httpClient.post("$baseUrl/internal/sporingslogg") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(
                SporingsloggJson(
                    identitetsnummer = identitetsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    dataTilLogging = dataTilLogging,
                ),
            )
        }
    }
}
