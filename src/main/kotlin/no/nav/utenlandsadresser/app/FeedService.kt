package no.nav.utenlandsadresser.app

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.persistence.FeedRepository
import org.slf4j.Logger

class FeedService(
    private val feedRepository: FeedRepository,
    private val registeroppslagClient: RegisteroppslagClient,
    private val logger: Logger,
) {
    suspend fun readNext(
        løpenummer: Løpenummer,
        orgnummer: Organisasjonsnummer
    ): Either<ReadFeedError, Pair<Identitetsnummer, Postadresse>> = either {
        val nextLøpenummer = Løpenummer(løpenummer.value + 1)
        val feedEvent = feedRepository.getFeedEvent(orgnummer, nextLøpenummer)
            ?: raise(ReadFeedError.FeedEventNotFound)

        val postadresse = registeroppslagClient.getPostadresse(feedEvent.identitetsnummer).getOrElse {
            when (it) {
                GetPostadresseError.IngenTilgang,
                GetPostadresseError.UgyldigForespørsel,
                GetPostadresseError.UkjentAdresse,
                is GetPostadresseError.UkjentFeil -> {
                    logger.error("Failed to get postadresse: {}", it)
                    raise(ReadFeedError.FailedToGetPostadresse)
                }
            }
        }

        feedEvent.identitetsnummer to postadresse
    }
}

sealed class ReadFeedError {
    data object FailedToGetPostadresse : ReadFeedError()
    data object FeedEventNotFound : ReadFeedError()
}