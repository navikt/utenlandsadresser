package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable

@Serializable
data class FeedResponseJsonV2(
    val abonnementId: String,
    val utenlandskPostadresse: UtenlandskPostadresseJson,
)
