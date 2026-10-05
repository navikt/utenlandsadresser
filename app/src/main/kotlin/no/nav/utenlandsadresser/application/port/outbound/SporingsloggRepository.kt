package no.nav.utenlandsadresser.application.port.outbound

import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import kotlin.time.Instant

interface SporingsloggRepository {
    suspend fun loggPostadresse(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        postadresse: Postadresse.Utenlandsk,
        tidspunktForUtlevering: Instant,
    )

    /** @param json gyldig JSON. Ugyldig JSON gir unntak. */
    suspend fun loggJson(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        json: String,
        tidspunktForUtlevering: Instant,
    )

    suspend fun loggUtenlandskId(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        utenlandskeIdentiteter: List<UtenlandskIdentitet>,
        tidspunktForUtlevering: Instant,
    )

    /** Sletter sporingslogger med tidspunkt for utlevering før [tidspunkt]. */
    suspend fun slettSporingsloggerFør(tidspunkt: Instant)
}
