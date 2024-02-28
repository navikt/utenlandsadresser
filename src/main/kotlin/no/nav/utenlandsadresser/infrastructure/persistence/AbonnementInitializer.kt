package no.nav.utenlandsadresser.infrastructure.persistence

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.InitAbonnementError

/**
 * Initialiserer et abonnement og eventuelt en postadresse. Implementasjonen må
 * passe på at databaseoperasjoner blir utført innenfor en transaksjon.
 */
interface AbonnementInitializer {
    suspend fun initAbonnement(abonnement: Abonnement, postadresse: Postadresse?): Either<InitAbonnementError, Unit>
}