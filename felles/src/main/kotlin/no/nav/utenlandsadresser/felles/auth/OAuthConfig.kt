package no.nav.utenlandsadresser.felles.auth

import com.sksamuel.hoplite.Masked

data class OAuthConfig(
    val tokenEndpoint: String,
    val clientId: String,
    val clientSecret: Masked,
    val grantType: String,
)
