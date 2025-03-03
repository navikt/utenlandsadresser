package no.nav.utenlandsadresser.app

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.micrometer.core.instrument.Counter
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.RegisteroppslagClient
import org.slf4j.Logger

class FeedService(
    private val feedRepository: FeedRepository,
    private val registeroppslagClient: RegisteroppslagClient,
    private val sporingsloggRepository: SporingsloggRepository,
    private val logger: Logger,
    private val utleverteUtenlandsadresserCounter: Counter,
) {
    suspend fun readNext(
        løpenummer: Løpenummer,
        orgnummer: Organisasjonsnummer,
    ): Either<ReadFeedError, Pair<FeedEvent.Outgoing, Postadresse.Utenlandsk?>> =
        either {
            val nextLøpenummer = Løpenummer(løpenummer.value + 1)
            val feedEvent =
                feedRepository.getFeedEvent(orgnummer, nextLøpenummer)
                    ?: raise(ReadFeedError.FeedEventNotFound)

            if (feedEvent.hendelsestype is Hendelsestype.Adressebeskyttelse) {
                return@either feedEvent to null
            }

            val postadresse =
                registeroppslagClient.getPostadresse(feedEvent.identitetsnummer).getOrElse {
                    when (it) {
                        GetPostadresseError.IngenTilgang,
                        GetPostadresseError.UgyldigForespørsel,
                        is GetPostadresseError.UkjentFeil,
                        -> {
                            logger.error(
                                "Fikk feil ved forsøk på å hente postadresse med organisasjonsnummer ${orgnummer.value} og løpenummer ${nextLøpenummer.value}: $it",
                            )
                            raise(ReadFeedError.FailedToGetPostadresse)
                        }

                        GetPostadresseError.UkjentAdresse -> null
                    }
                }

            feedEvent to
                when (postadresse) {
                    null,
                    is Postadresse.Norsk,
                    -> null

                    is Postadresse.Utenlandsk ->
                        postadresse.also {
                            sporingsloggRepository.loggPostadresse(
                                feedEvent.identitetsnummer,
                                orgnummer,
                                postadresse,
                            )
                            utleverteUtenlandsadresserCounter.increment()
                        }
                }
        }
}

sealed class ReadFeedError {
    data object FailedToGetPostadresse : ReadFeedError()

    data object FeedEventNotFound : ReadFeedError()
}
