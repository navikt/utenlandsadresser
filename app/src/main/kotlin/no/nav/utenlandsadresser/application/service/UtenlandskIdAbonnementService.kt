package no.nav.utenlandsadresser.application.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import no.nav.utenlandsadresser.application.port.outbound.DeleteAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.HentUtenlandskId
import no.nav.utenlandsadresser.application.port.outbound.InitAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementInitializer
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.Uuid

class UtenlandskIdAbonnementService(
    private val abonnementRepository: UtenlandskIdAbonnementRepository,
    private val hentUtenlandskId: HentUtenlandskId,
    private val abonnementInitializer: UtenlandskIdAbonnementInitializer,
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
                hentUtenlandskId.hentUtenlandskIdentitet(identitetsnummer).getOrElse {
                    logger.error("Greide ikke å hente utenlandsk id ved start av abonnement: {}", it)
                    raise(StartUtenlandskIdAbonnementError.FailedToGetUtenlandskId)
                }

            abonnementInitializer
                .initAbonnement(abonnement, harUtenlandskId = utenlandskeIdentiteter.isNotEmpty())
                .mapLeft {
                    when (it) {
                        is InitAbonnementError.AbonnementAlreadyExists -> {
                            StartUtenlandskIdAbonnementError.AbonnementAlreadyExists(it.abonnement)
                        }
                    }
                }.bind()
        }

    suspend fun stopAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StoppAbonnementError, Unit> =
        abonnementRepository.deleteAbonnement(abonnementId, organisasjonsnummer).mapLeft {
            when (it) {
                DeleteAbonnementError.NotFound -> StoppAbonnementError.AbonnementNotFound
            }
        }
}

sealed class StartUtenlandskIdAbonnementError {
    data class AbonnementAlreadyExists(
        val abonnement: Abonnement,
    ) : StartUtenlandskIdAbonnementError()

    data object FailedToGetUtenlandskId : StartUtenlandskIdAbonnementError()
}
