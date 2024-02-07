package no.nav.utenlandsadresser.infrastructure.persistence

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.CreateAbonnementError

interface AbonnementRepository {
    fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Unit>
    fun deleteAbonnement(identitetsnummer: Identitetsnummer, organisasjonsnummer: Organisasjonsnummer)
    fun getAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement>
}
