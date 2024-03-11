package no.nav.utenlandsadresser.infrastructure.persistence

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import java.util.*

interface AbonnementRepository {
    suspend fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Abonnement>
    suspend fun deleteAbonnement(
        abonnementId: UUID,
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