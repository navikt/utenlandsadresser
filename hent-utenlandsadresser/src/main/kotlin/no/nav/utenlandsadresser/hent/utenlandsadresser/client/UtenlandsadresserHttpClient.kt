package no.nav.utenlandsadresser.hent.utenlandsadresser.client

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.felles.sporingslogg.SporingsloggJson
import no.nav.utenlandsadresser.hent.utenlandsadresser.Sporingslogg
import no.nav.utenlandsadresser.hent.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.hent.utenlandsadresser.domain.Organisasjonsnummer
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
                    identitetsnummer = identitetsnummer.value,
                    organisasjonsnummer = organisasjonsnummer.value,
                    dataTilLogging = dataTilLogging,
                ),
            )
        }
    }
}
