package no.nav.utenlandsadresser.infrastructure.persistence

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

interface AbonnementRepository {
    suspend fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Abonnement>
    suspend fun deleteAbonnement(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer
    ): Either<DeleteAbonnementError, Unit>

    suspend fun getAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement>
}

sealed class CreateAbonnementError {
    data class AlreadyExists(val abonnement: Abonnement) : CreateAbonnementError()
}

sealed class DeleteAbonnementError {
    data object NotFound : DeleteAbonnementError()
}