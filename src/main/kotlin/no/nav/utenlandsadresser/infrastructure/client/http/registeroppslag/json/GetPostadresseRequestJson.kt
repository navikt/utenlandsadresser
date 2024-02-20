package no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.json

import kotlinx.serialization.Serializable

@Serializable
data class GetPostadresseRequestJson(
    val ident: String,
)