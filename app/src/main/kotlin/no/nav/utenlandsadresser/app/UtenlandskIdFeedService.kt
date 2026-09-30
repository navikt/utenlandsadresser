package no.nav.utenlandsadresser.app

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.micrometer.core.instrument.Counter
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.infrastructure.client.HentUtenlandskId
import org.slf4j.Logger

class UtenlandskIdFeedService(
    private val feedRepository: UtenlandskIdFeedRepository,
    private val hentUtenlandskId: HentUtenlandskId,
    private val sporingsloggRepository: SporingsloggRepository,
    private val logger: Logger,
    private val utleverteUtenlandskeIdCounter: Counter,
) {
    /**
     * Leser hendelsen etter gitt løpenummer og henter gjeldende utenlandske id-er for personen.
     *
     * Listen kan være tom, for eksempel om id-en er opphørt etter at hendelsen ble lagt på feeden.
     * Bare ikke-tomme lister blir utlevert, og bare de blir sporingslogget og talt.
     */
    suspend fun readNext(
        løpenummer: Løpenummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<ReadUtenlandskIdFeedError, Pair<UtenlandskIdFeedEvent.Outgoing, List<UtenlandskIdentitet>>> =
        either {
            val nextLøpenummer = Løpenummer(løpenummer.value + 1)
            val feedEvent =
                feedRepository.getFeedEvent(organisasjonsnummer, nextLøpenummer)
                    ?: raise(ReadUtenlandskIdFeedError.FeedEventNotFound)

            val utenlandskeIdentiteter =
                hentUtenlandskId.hentUtenlandskIdentitet(feedEvent.identitetsnummer).getOrElse {
                    logger.error(
                        "Greide ikke å hente utenlandsk id for organisasjonsnummer {} og løpenummer {}: {}",
                        organisasjonsnummer.value,
                        nextLøpenummer.value,
                        it,
                    )
                    raise(ReadUtenlandskIdFeedError.FailedToGetUtenlandskId)
                }

            if (utenlandskeIdentiteter.isNotEmpty()) {
                sporingsloggRepository.loggUtenlandskId(
                    feedEvent.identitetsnummer,
                    organisasjonsnummer,
                    utenlandskeIdentiteter,
                )
                utleverteUtenlandskeIdCounter.increment()
            }

            feedEvent to utenlandskeIdentiteter
        }
}

sealed class ReadUtenlandskIdFeedError {
    data object FailedToGetUtenlandskId : ReadUtenlandskIdFeedError()

    data object FeedEventNotFound : ReadUtenlandskIdFeedError()
}
