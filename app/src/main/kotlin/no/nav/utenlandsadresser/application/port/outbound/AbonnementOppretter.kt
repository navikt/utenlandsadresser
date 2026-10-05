package no.nav.utenlandsadresser.application.port.outbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Postadresse

/**
 * Oppretter et abonnement og eventuelt et feed-event for postadressen. Implementasjonen må
 * passe på at databaseoperasjoner blir utført innenfor en transaksjon.
 */
interface AbonnementOppretter {
    suspend fun opprettMedEvent(
        abonnement: Abonnement,
        postadresse: Postadresse?,
    ): Either<OpprettAbonnementMedEventError, Abonnement>
}
