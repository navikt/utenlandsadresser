package no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetPostadresseRequestJson(
    val ident: String,
    val filtrerAdressebeskyttelse: Set<RegisteroppslagAdressebeskyttelse>,
)

@Serializable
enum class RegisteroppslagAdressebeskyttelse {
    @SerialName("strengt_fortrolig_utland")
    STRENGT_FORTROLIG_UTLAND,

    @SerialName("strengt_fortrolig")
    STRENGT_FORTROLIG,

    @SerialName("fortrolig")
    FORTROLIG,
}
