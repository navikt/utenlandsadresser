package no.nav.utenlandsadresser.adapter.inbound.web.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Abonnement

@Serializable
data class StartAbonnementResponseJson(
    val abonnementId: String,
) {
    companion object {
        fun fromDomain(abonnement: Abonnement): StartAbonnementResponseJson = StartAbonnementResponseJson(abonnement.id.toString())
    }
}
