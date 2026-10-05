package no.nav.utenlandsadresser.application.port.outbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Postadresse

/**
 * Initialiserer et abonnement og eventuelt en postadresse. Implementasjonen må
 * passe på at databaseoperasjoner blir utført innenfor en transaksjon.
 */
interface AbonnementInitializer {
    suspend fun initAbonnement(
        abonnement: Abonnement,
        postadresse: Postadresse?,
    ): Either<InitAbonnementError, Abonnement>
}
