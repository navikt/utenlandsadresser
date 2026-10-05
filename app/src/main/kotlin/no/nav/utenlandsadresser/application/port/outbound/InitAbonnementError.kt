package no.nav.utenlandsadresser.application.port.outbound

import no.nav.utenlandsadresser.domain.Abonnement

sealed class InitAbonnementError {
    data class AbonnementAlreadyExists(
        val abonnement: Abonnement,
    ) : InitAbonnementError()
}
