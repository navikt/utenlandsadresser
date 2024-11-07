package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable

@Serializable
data class StartAbonnementRequestJson(
    val identitetsnummer: String,
)
