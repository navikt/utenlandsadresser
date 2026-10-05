package no.nav.utenlandsadresser.application.port.outbound

import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent

interface UtenlandskIdFeedRepository {
    suspend fun hentFeedEvent(
        organisasjonsnummer: Organisasjonsnummer,
        løpenummer: Løpenummer,
    ): UtenlandskIdFeedEvent.Outgoing?
}
