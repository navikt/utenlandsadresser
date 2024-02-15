package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable

@Serializable
data class StoppAbonnementJson(
    val identitetsnummer: String,
)