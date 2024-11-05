package no.nav.utenlandsadresser.hent.utenlandsadresser

import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.AdresseJson

interface OppdaterUtenlandsadresse {
    suspend fun oppdaterUtenlandsadresse(
        identitetsnummer: String,
        utenlandskAdresse: AdresseJson,
    )
}
