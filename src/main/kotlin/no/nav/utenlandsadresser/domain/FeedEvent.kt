package no.nav.utenlandsadresser.domain

sealed class FeedEvent {
    abstract val identitetsnummer: Identitetsnummer

    data class Incoming(
        override val identitetsnummer: Identitetsnummer,
        val organisasjonsnummer: Organisasjonsnummer,
    ) : FeedEvent()

    data class Outgoing(
        override val identitetsnummer: Identitetsnummer,
    ) : FeedEvent()
}

