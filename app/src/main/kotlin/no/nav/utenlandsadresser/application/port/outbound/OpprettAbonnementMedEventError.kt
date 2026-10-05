package no.nav.utenlandsadresser.application.port.outbound

import no.nav.utenlandsadresser.domain.Abonnement

sealed class OpprettAbonnementMedEventError {
    data class AbonnementFinnesAllerede(
        val abonnement: Abonnement,
    ) : OpprettAbonnementMedEventError()
}
