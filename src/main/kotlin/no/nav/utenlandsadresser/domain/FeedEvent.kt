package no.nav.utenlandsadresser.domain

import java.util.*

sealed class FeedEvent {
    abstract val identitetsnummer: Identitetsnummer
    abstract val abonnementId: UUID

    data class Incoming(
        override val identitetsnummer: Identitetsnummer,
        override val abonnementId: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
    ) : FeedEvent()

    data class Outgoing(
        override val identitetsnummer: Identitetsnummer,
        override val abonnementId: UUID,
    ) : FeedEvent()
}

