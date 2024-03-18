package no.nav.utenlandsadresser.domain

import java.util.*

sealed class FeedEvent {
    abstract val identitetsnummer: Identitetsnummer
    abstract val abonnementId: UUID
    abstract val hendelsestype: Hendelsestype

    data class Incoming(
        override val identitetsnummer: Identitetsnummer,
        override val abonnementId: UUID,
        override val hendelsestype: Hendelsestype,
        val organisasjonsnummer: Organisasjonsnummer,
    ) : FeedEvent()

    data class Outgoing(
        override val identitetsnummer: Identitetsnummer,
        override val abonnementId: UUID,
        override val hendelsestype: Hendelsestype,
    ) : FeedEvent()
}

