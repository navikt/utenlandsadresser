package no.nav.utenlandsadresser.infrastructure.client.http.maskinporten.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MaskinportenTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    val scope: String,
)