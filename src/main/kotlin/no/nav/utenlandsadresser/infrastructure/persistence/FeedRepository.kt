package no.nav.utenlandsadresser.infrastructure.persistence

import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

interface FeedRepository {
    suspend fun createFeedEvent(feedEvent: FeedEvent.Incoming)
    suspend fun getFeedEvent(organisasjonsnummer: Organisasjonsnummer, løpenummer: Løpenummer): FeedEvent.Outgoing?
}