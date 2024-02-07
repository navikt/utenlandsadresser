package no.nav.utenlandsadresser.infrastructure.persistence

import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Løpenummer

interface FeedRepository {
    fun createFeedEvent(feedEvent: FeedEvent.Incoming)
    fun getFeedEvent(organisasjonsnummer: Organisasjonsnummer, løpenummer: Løpenummer): FeedEvent.Outgoing?
}