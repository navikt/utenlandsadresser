package no.nav.utenlandsadresser.infrastructure.route.internal.json

import kotlinx.serialization.Serializable

@Serializable
data class RegOppslagRequest(
    val fnr: String,
)
