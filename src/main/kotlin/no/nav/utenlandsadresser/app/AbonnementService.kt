package no.nav.utenlandsadresser.app

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.CreateAbonnementError

class AbonnementService(
    private val abbonementRepository: AbonnementRepository,
) {
    fun startAbonnement(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer
    ): Either<StartAbonnementError, Unit> = either {
        val abonnement = Abonnement(
            organisasjonsnummer = organisasjonsnummer,
            identitetsnummer = identitetsnummer,
            opprettet = Clock.System.now(),
        )

        abbonementRepository.createAbonnement(abonnement).getOrElse {
            when (it) {
                CreateAbonnementError.AlreadyExists -> raise(StartAbonnementError.AbonnementAlreadyExists)
            }
        }
    }

    fun stopAbonnement(identitetsnummer: Identitetsnummer, organisasjonsnummer: Organisasjonsnummer) {
        abbonementRepository.deleteAbonnement(identitetsnummer, organisasjonsnummer)
    }

    fun hentAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> {
        return abbonementRepository.getAbonnementer(identitetsnummer)
    }
    sealed class StartAbonnementError {
        data object AbonnementAlreadyExists : StartAbonnementError()
    }
}
