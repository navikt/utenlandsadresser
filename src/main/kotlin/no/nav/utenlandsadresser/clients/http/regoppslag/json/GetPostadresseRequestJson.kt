package no.nav.utenlandsadresser.clients.http.regoppslag.json

import kotlinx.serialization.Serializable

@Serializable
data class GetPostadresseRequestJson(
    val ident: String,
    val tema: String,
)