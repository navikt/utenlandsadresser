package no.nav.utenlandsadresser.infrastructure.persistence

import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Løpenummer

interface FeedRepository {
    fun createFeedEvent(feedEvent: FeedEvent.Incoming)
    fun getFeedEvent(clientId: ClientId, løpenummer: Løpenummer): FeedEvent.Outgoing?
}