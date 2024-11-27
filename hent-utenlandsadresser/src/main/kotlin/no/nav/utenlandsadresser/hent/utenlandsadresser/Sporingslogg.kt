package no.nav.utenlandsadresser.hent.utenlandsadresser

import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

interface Sporingslogg {
    suspend fun logg(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        dataTilLogging: JsonElement,
    )
}
