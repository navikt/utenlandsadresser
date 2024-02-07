package no.nav.utenlandsadresser.domain

sealed class FeedEvent {
    abstract val identitetsnummer: Identitetsnummer
    abstract val clientId: ClientId

    data class Incoming(
        override val identitetsnummer: Identitetsnummer,
        override val clientId: ClientId,
    ) : FeedEvent()

    data class Outgoing(
        override val identitetsnummer: Identitetsnummer,
        override val clientId: ClientId,
        val løpenummer: Løpenummer,
    ) : FeedEvent()
}

