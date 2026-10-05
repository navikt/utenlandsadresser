package no.nav.utenlandsadresser.adapter.inbound.web.json

import kotlinx.serialization.Serializable

@Serializable
data class StoppAbonnementJson(
    val abonnementId: String,
)
