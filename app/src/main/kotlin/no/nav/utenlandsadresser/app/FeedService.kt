package no.nav.utenlandsadresser.app

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.micrometer.core.instrument.Counter
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import org.slf4j.Logger

class FeedService(
    private val feedRepository: FeedRepository,
    private val abonnementRepository: AbonnementRepository,
    private val registeroppslagClient: RegisteroppslagClient,
    private val sporingsloggRepository: SporingsloggRepository,
    private val logger: Logger,
    private val utleverteUtenlandsadresserCounter: Counter,
    private val stoppetAbonnementCounter: Counter,
) {
    /**
     * Leser hendelsen etter gitt løpenummer og henter gjeldende postadresse for personen.
     *
     * Adressebeskyttelse leveres alltid, så mottakeren sletter adressen. For andre hendelser på et abonnement
     * som er stoppet, returneres hendelsen uten adresse. Da deler vi ikke adressen, men mottakeren kan
     * fortsatt gå videre til neste løpenummer.
     */
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

            if (!abonnementRepository.finnesAbonnement(feedEvent.abonnementId, orgnummer)) {
                logger.info(
                    "Abonnementet er stoppet. Leverer hendelse uten adresse for organisasjonsnummer {} og løpenummer {}",
                    orgnummer.value,
                    nextLøpenummer.value,
                )
                stoppetAbonnementCounter.increment()
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

                        GetPostadresseError.FalskIdentiet,
                        GetPostadresseError.UkjentAdresse,
                        -> {
                            null
                        }
                    }
                }

            feedEvent to
                when (postadresse) {
                    null,
                    is Postadresse.Norsk,
                    -> {
                        null
                    }

                    is Postadresse.Utenlandsk -> {
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
}

sealed class ReadFeedError {
    data object FailedToGetPostadresse : ReadFeedError()

    data object FeedEventNotFound : ReadFeedError()
}
