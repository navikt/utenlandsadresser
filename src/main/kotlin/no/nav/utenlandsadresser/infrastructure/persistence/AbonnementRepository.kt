package no.nav.utenlandsadresser.infrastructure.persistence

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.CreateAbonnementError

interface AbonnementRepository {
    fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Unit>
    fun deleteAbonnement(identitetsnummer: Identitetsnummer, clientId: ClientId)
    fun getAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement>
}
