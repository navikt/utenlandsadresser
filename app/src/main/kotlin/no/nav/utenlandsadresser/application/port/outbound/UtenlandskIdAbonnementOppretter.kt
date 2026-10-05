package no.nav.utenlandsadresser.application.port.outbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement

/**
 * Oppretter et abonnement på utenlandsk id og eventuelt en feed-hendelse. Implementasjonen må
 * passe på at databaseoperasjonene blir utført innenfor én transaksjon.
 */
interface UtenlandskIdAbonnementOppretter {
    suspend fun opprettMedEvent(
        abonnement: Abonnement,
        harUtenlandskId: Boolean,
    ): Either<OpprettAbonnementMedEventError, Abonnement>
}
