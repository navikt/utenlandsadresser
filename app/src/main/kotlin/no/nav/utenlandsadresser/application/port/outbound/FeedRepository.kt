package no.nav.utenlandsadresser.application.port.outbound

import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import kotlin.time.Duration

interface FeedRepository {
    suspend fun hentFeedEvent(
        organisasjonsnummer: Organisasjonsnummer,
        løpenummer: Løpenummer,
    ): FeedEvent.Outgoing?

    /**
     * Legger hendelsene på feeden. En hendelse hoppes over hvis en hendelse med samme identitetsnummer,
     * abonnement og hendelsestype er lagt på feeden innenfor [vindu]. Sjekk og innsetting skjer i én transaksjon.
     */
    suspend fun opprettUtenDuplikater(
        events: List<FeedEvent.Incoming>,
        vindu: Duration,
    )
}
