package no.nav.utenlandsadresser.infrastructure.persistence

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Identitetsnummer

interface AbonnementRepository {
    fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Unit>
    fun deleteAbonnement(identitetsnummer: Identitetsnummer, organisasjonsnummer: Organisasjonsnummer): Either<DeleteAbonnementError, Unit>
    fun getAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement>
}

sealed class CreateAbonnementError {
    data object AlreadyExists : CreateAbonnementError()
}

sealed class DeleteAbonnementError {
    data object NotFound : DeleteAbonnementError()
}