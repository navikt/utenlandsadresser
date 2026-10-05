package no.nav.utenlandsadresser.application.port.inbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet

interface LesUtenlandskIdFeed {
    /**
     * Leser hendelsen etter gitt løpenummer og henter gjeldende utenlandske id-er for personen.
     *
     * Listen kan være tom, for eksempel om id-en er opphørt etter at hendelsen ble lagt på feeden.
     * Bare ikke-tomme lister blir utlevert, og bare de blir sporingslogget og talt.
     *
     * Er abonnementet stoppet, returneres hendelsen med tom liste uten oppslag.
     */
    suspend fun lesNeste(
        løpenummer: Løpenummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<LesUtenlandskIdFeedError, Pair<UtenlandskIdFeedEvent.Outgoing, List<UtenlandskIdentitet>>>
}

sealed class LesUtenlandskIdFeedError {
    data object KunneIkkeHenteUtenlandskId : LesUtenlandskIdFeedError()

    data object FeedEventIkkeFunnet : LesUtenlandskIdFeedError()
}
