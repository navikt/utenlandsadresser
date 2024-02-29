package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable

@Serializable
data class StoppAbonnementJsonV2(
    val abonnementId: String,
)
