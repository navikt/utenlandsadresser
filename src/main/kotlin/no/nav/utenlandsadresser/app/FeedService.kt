package no.nav.utenlandsadresser.app

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.persistence.FeedRepository

class FeedService(
    private val feedRepository: FeedRepository,
    private val registeroppslagClient: RegisteroppslagClient,
) {
    suspend fun readFeed(
        løpenummer: Løpenummer,
        orgnummer: Organisasjonsnummer
    ): Either<ReadFeedError, Postadresse.Utenlandsk> = either {
        val feedEvent = feedRepository.getFeedEvent(orgnummer, løpenummer)
            ?: raise(ReadFeedError.FeedEventNotFound)

        return registeroppslagClient.getPostadresse(feedEvent.identitetsnummer).fold({
            when (it) {
                GetPostadresseError.IngenTilgang,
                GetPostadresseError.UgyldigForespørsel,
                GetPostadresseError.UkjentAdresse,
                is GetPostadresseError.UkjentFeil -> raise(ReadFeedError.FailedToGetPostadresse)
            }
        }, {
            when (it) {
                is Postadresse.Utenlandsk -> it
                Postadresse.Empty,
                is Postadresse.Norsk -> raise(ReadFeedError.PostadresseNotFound)
            }.right()
        })
    }
}

sealed class ReadFeedError {
    data object FailedToGetPostadresse : ReadFeedError()
    data object PostadresseNotFound : ReadFeedError()
    data object FeedEventNotFound : ReadFeedError()
}