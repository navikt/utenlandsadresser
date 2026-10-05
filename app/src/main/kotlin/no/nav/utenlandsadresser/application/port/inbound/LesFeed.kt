package no.nav.utenlandsadresser.application.port.inbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse

interface LesFeed {
    /**
     * Leser hendelsen etter gitt løpenummer og henter gjeldende postadresse for personen.
     *
     * Adressebeskyttelse leveres alltid, så mottakeren sletter adressen. For andre hendelser på et abonnement
     * som er stoppet, returneres hendelsen uten adresse. Da deler vi ikke adressen, men mottakeren kan
     * fortsatt gå videre til neste løpenummer.
     */
    suspend fun lesNeste(
        løpenummer: Løpenummer,
        orgnummer: Organisasjonsnummer,
    ): Either<LesFeedError, Pair<FeedEvent.Outgoing, Postadresse.Utenlandsk?>>
}

sealed class LesFeedError {
    data object KunneIkkeHentePostadresse : LesFeedError()

    data object FeedEventIkkeFunnet : LesFeedError()
}
