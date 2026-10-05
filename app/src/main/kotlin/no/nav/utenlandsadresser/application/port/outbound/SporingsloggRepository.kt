package no.nav.utenlandsadresser.application.port.outbound

import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import kotlin.time.Duration
import kotlin.time.Instant

interface SporingsloggRepository {
    suspend fun loggPostadresse(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        postadresse: Postadresse.Utenlandsk,
        tidspunktForUtlevering: Instant,
    )

    suspend fun loggJson(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        json: JsonElement,
        tidspunktForUtlevering: Instant,
    )

    suspend fun loggUtenlandskId(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        utenlandskeIdentiteter: List<UtenlandskIdentitet>,
        tidspunktForUtlevering: Instant,
    )

    suspend fun slettSporingsloggerEldreEnn(duration: Duration)
}
