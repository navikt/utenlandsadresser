package no.nav.utenlandsadresser.application.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import no.nav.utenlandsadresser.application.port.inbound.LesFeed
import no.nav.utenlandsadresser.application.port.inbound.LesFeedError
import no.nav.utenlandsadresser.application.port.outbound.AbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.Feed
import no.nav.utenlandsadresser.application.port.outbound.FeedRepository
import no.nav.utenlandsadresser.application.port.outbound.HentPostadresseError
import no.nav.utenlandsadresser.application.port.outbound.Metrikker
import no.nav.utenlandsadresser.application.port.outbound.PostadresseOppslag
import no.nav.utenlandsadresser.application.port.outbound.SporingsloggRepository
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class FeedService(
    private val feedRepository: FeedRepository,
    private val abonnementRepository: AbonnementRepository,
    private val postadresseOppslag: PostadresseOppslag,
    private val sporingsloggRepository: SporingsloggRepository,
    private val clock: Clock,
) : LesFeed {
    private val logger = LoggerFactory.getLogger(FeedService::class.java)

    context(metrikker: Metrikker)
    override suspend fun lesNeste(
        løpenummer: Løpenummer,
        orgnummer: Organisasjonsnummer,
    ): Either<LesFeedError, Pair<FeedEvent.Outgoing, Postadresse.Utenlandsk?>> =
        either {
            val nextLøpenummer = Løpenummer(løpenummer.value + 1)
            val feedEvent =
                feedRepository.hentFeedEvent(orgnummer, nextLøpenummer)
                    ?: raise(LesFeedError.FeedEventIkkeFunnet)

            if (feedEvent.hendelsestype is Hendelsestype.Adressebeskyttelse) {
                return@either feedEvent to null
            }

            if (!abonnementRepository.finnesAbonnement(feedEvent.abonnementId, orgnummer)) {
                logger.info(
                    "Abonnementet er stoppet. Leverer hendelse uten adresse for organisasjonsnummer {} og løpenummer {}",
                    orgnummer.value,
                    nextLøpenummer.value,
                )
                metrikker.stoppetAbonnementLest(Feed.POSTADRESSE)
                return@either feedEvent to null
            }

            val postadresse =
                postadresseOppslag.hentPostadresse(feedEvent.identitetsnummer).getOrElse {
                    when (it) {
                        HentPostadresseError.IngenTilgang,
                        HentPostadresseError.UgyldigForespørsel,
                        is HentPostadresseError.UkjentFeil,
                        -> {
                            logger.error(
                                "Fikk feil ved forsøk på å hente postadresse med organisasjonsnummer ${orgnummer.value} og løpenummer ${nextLøpenummer.value}: $it",
                            )
                            raise(LesFeedError.KunneIkkeHentePostadresse)
                        }

                        HentPostadresseError.FalskIdentiet,
                        HentPostadresseError.UkjentAdresse,
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
                                clock.now(),
                            )
                            metrikker.utlevert(Feed.POSTADRESSE)
                        }
                    }
                }
        }
}
