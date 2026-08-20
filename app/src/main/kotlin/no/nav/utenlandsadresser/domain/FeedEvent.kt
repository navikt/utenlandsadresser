package no.nav.utenlandsadresser.domain

import kotlin.uuid.Uuid

sealed class FeedEvent {
    abstract val identitetsnummer: Identitetsnummer
    abstract val abonnementId: Uuid
    abstract val hendelsestype: Hendelsestype

    data class Incoming(
        override val identitetsnummer: Identitetsnummer,
        override val abonnementId: Uuid,
        override val hendelsestype: Hendelsestype,
        val organisasjonsnummer: Organisasjonsnummer,
    ) : FeedEvent()

    data class Outgoing(
        override val identitetsnummer: Identitetsnummer,
        override val abonnementId: Uuid,
        override val hendelsestype: Hendelsestype,
    ) : FeedEvent()
}
