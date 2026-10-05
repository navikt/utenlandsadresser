package no.nav.utenlandsadresser.application.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import no.nav.utenlandsadresser.application.port.outbound.Feed
import no.nav.utenlandsadresser.application.port.outbound.Metrikker
import no.nav.utenlandsadresser.application.port.outbound.SporingsloggRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdFeedRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdOppslag
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class UtenlandskIdFeedService(
    private val feedRepository: UtenlandskIdFeedRepository,
    private val abonnementRepository: UtenlandskIdAbonnementRepository,
    private val utenlandskIdOppslag: UtenlandskIdOppslag,
    private val sporingsloggRepository: SporingsloggRepository,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(UtenlandskIdFeedService::class.java)

    /**
     * Leser hendelsen etter gitt løpenummer og henter gjeldende utenlandske id-er for personen.
     *
     * Listen kan være tom, for eksempel om id-en er opphørt etter at hendelsen ble lagt på feeden.
     * Bare ikke-tomme lister blir utlevert, og bare de blir sporingslogget og talt.
     *
     * Er abonnementet stoppet, returneres hendelsen med tom liste uten oppslag i PDL.
     */
    context(metrikker: Metrikker)
    suspend fun lesNeste(
        løpenummer: Løpenummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<LesUtenlandskIdFeedError, Pair<UtenlandskIdFeedEvent.Outgoing, List<UtenlandskIdentitet>>> =
        either {
            val nextLøpenummer = Løpenummer(løpenummer.value + 1)
            val feedEvent =
                feedRepository.hentFeedEvent(organisasjonsnummer, nextLøpenummer)
                    ?: raise(LesUtenlandskIdFeedError.FeedEventIkkeFunnet)

            if (!abonnementRepository.finnesAbonnement(feedEvent.abonnementId, organisasjonsnummer)) {
                logger.info(
                    "Abonnementet er stoppet. Leverer hendelse uten utenlandsk id for organisasjonsnummer {} og løpenummer {}",
                    organisasjonsnummer.value,
                    nextLøpenummer.value,
                )
                metrikker.stoppetAbonnementLest(Feed.UTENLANDSK_ID)
                return@either feedEvent to emptyList()
            }

            val utenlandskeIdentiteter =
                utenlandskIdOppslag.hentUtenlandskIdentitet(feedEvent.identitetsnummer).getOrElse {
                    logger.error(
                        "Greide ikke å hente utenlandsk id for organisasjonsnummer {} og løpenummer {}: {}",
                        organisasjonsnummer.value,
                        nextLøpenummer.value,
                        it,
                    )
                    raise(LesUtenlandskIdFeedError.KunneIkkeHenteUtenlandskId)
                }

            if (utenlandskeIdentiteter.isNotEmpty()) {
                sporingsloggRepository.loggUtenlandskId(
                    feedEvent.identitetsnummer,
                    organisasjonsnummer,
                    utenlandskeIdentiteter,
                    clock.now(),
                )
                metrikker.utlevert(Feed.UTENLANDSK_ID)
            }

            feedEvent to utenlandskeIdentiteter
        }
}

sealed class LesUtenlandskIdFeedError {
    data object KunneIkkeHenteUtenlandskId : LesUtenlandskIdFeedError()

    data object FeedEventIkkeFunnet : LesUtenlandskIdFeedError()
}
