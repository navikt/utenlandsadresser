package no.nav.utenlandsadresser.application.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementMedEventError
import no.nav.utenlandsadresser.application.port.outbound.SlettAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementOppretter
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdOppslag
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.Uuid

class UtenlandskIdAbonnementService(
    private val abonnementRepository: UtenlandskIdAbonnementRepository,
    private val utenlandskIdOppslag: UtenlandskIdOppslag,
    private val abonnementOppretter: UtenlandskIdAbonnementOppretter,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(UtenlandskIdAbonnementService::class.java)

    /**
     * Starter et abonnement på utenlandsk id. Oppslaget mot kilden gjøres før databasetransaksjonen,
     * slik at transaksjonen ikke holdes åpen under nettverkskallet.
     *
     * Har personen utenlandsk id, legges det en hendelse på feeden. Feeden lagrer ikke selve id-en.
     */
    suspend fun startAbonnement(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StartUtenlandskIdAbonnementError, Abonnement> =
        either {
            val abonnement =
                Abonnement(
                    id = Uuid.random(),
                    organisasjonsnummer = organisasjonsnummer,
                    identitetsnummer = identitetsnummer,
                    opprettet = clock.now(),
                )

            val utenlandskeIdentiteter =
                utenlandskIdOppslag.hentUtenlandskIdentitet(identitetsnummer).getOrElse {
                    logger.error("Greide ikke å hente utenlandsk id ved start av abonnement: {}", it)
                    raise(StartUtenlandskIdAbonnementError.KunneIkkeHenteUtenlandskId)
                }

            abonnementOppretter
                .opprettMedEvent(abonnement, harUtenlandskId = utenlandskeIdentiteter.isNotEmpty())
                .mapLeft {
                    when (it) {
                        is OpprettAbonnementMedEventError.AbonnementFinnesAllerede -> {
                            StartUtenlandskIdAbonnementError.AbonnementFinnesAllerede(it.abonnement)
                        }
                    }
                }.bind()
        }

    suspend fun stoppAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StoppAbonnementError, Unit> =
        abonnementRepository.slettAbonnement(abonnementId, organisasjonsnummer).mapLeft {
            when (it) {
                SlettAbonnementError.IkkeFunnet -> StoppAbonnementError.AbonnementIkkeFunnet
            }
        }
}

sealed class StartUtenlandskIdAbonnementError {
    data class AbonnementFinnesAllerede(
        val abonnement: Abonnement,
    ) : StartUtenlandskIdAbonnementError()

    data object KunneIkkeHenteUtenlandskId : StartUtenlandskIdAbonnementError()
}
