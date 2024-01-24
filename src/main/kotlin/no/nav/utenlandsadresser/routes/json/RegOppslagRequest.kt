package no.nav.utenlandsadresser.routes.json

import kotlinx.serialization.Serializable

@Serializable
data class RegOppslagRequest(
    val fnr: String,
)