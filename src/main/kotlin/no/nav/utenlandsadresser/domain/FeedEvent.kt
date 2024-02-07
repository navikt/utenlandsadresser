package no.nav.utenlandsadresser.domain

sealed class FeedEvent {
    abstract val identitetsnummer: Identitetsnummer
    abstract val organisasjonsnummer: Organisasjonsnummer

    data class Incoming(
        override val identitetsnummer: Identitetsnummer,
        override val organisasjonsnummer: Organisasjonsnummer,
    ) : FeedEvent()

    data class Outgoing(
        override val identitetsnummer: Identitetsnummer,
        override val organisasjonsnummer: Organisasjonsnummer,
        val løpenummer: Løpenummer,
    ) : FeedEvent()
}

