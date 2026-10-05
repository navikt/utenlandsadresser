package no.nav.utenlandsadresser.application.port.inbound

import no.nav.utenlandsadresser.domain.Livshendelse

/**
 * Legger en hendelse på feeden for hvert abonnement på personen.
 */
interface HåndterLivshendelse {
    suspend fun håndter(livshendelse: Livshendelse)
}
