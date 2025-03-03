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
    companion object {
        val ignorerteLøpenummer =
            mapOf(
                (Organisasjonsnummer("974761076") to Løpenummer(9136)) to
                    "03-03-2025: Ignorert pga. falsk identitetsnummer. Kan fjernes når registeroppslag håndterer falske identiteter med å returnere 404.",
            )
    }

    suspend fun readNext(
        løpenummer: Løpenummer,
        orgnummer: Organisasjonsnummer,
    ): Either<ReadFeedError, Pair<FeedEvent.Outgoing, Postadresse.Utenlandsk?>> =
        either {
            val nextLøpenummer = Løpenummer(løpenummer.value + 1)
            val feedEvent =
                feedRepository.getFeedEvent(orgnummer, nextLøpenummer)
                    ?: raise(ReadFeedError.FeedEventNotFound)

            if (ignorerteLøpenummer.containsKey(orgnummer to nextLøpenummer)) {
                logger.warn(
                    "Feed event $løpenummer for organisasjon $orgnummer ble ignorert: ${ignorerteLøpenummer[orgnummer to nextLøpenummer]}",
                )
                return@either feedEvent to null
            }

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
