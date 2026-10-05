package no.nav.utenlandsadresser.application.port.inbound

import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

interface SkrivSporingslogg {
    /**
     * Logger at [json] er utlevert til [organisasjonsnummer]. Tidspunktet for utlevering er nå.
     *
     * @param json gyldig JSON med dataene som er utlevert.
     */
    suspend fun skriv(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        json: String,
    )
}
