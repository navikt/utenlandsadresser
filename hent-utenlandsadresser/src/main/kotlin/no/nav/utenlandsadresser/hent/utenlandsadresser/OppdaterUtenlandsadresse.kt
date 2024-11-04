package no.nav.utenlandsadresser.hent.utenlandsadresser

import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.UtenlandskAdresseJson

interface OppdaterUtenlandsadresse {
    suspend fun oppdaterUtenlandsadresse(
        identitetsnummer: String,
        utenlandskAdresse: UtenlandskAdresseJson,
    )
}
