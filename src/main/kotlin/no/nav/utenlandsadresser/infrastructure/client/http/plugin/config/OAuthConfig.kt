package no.nav.utenlandsadresser.infrastructure.client.http.plugin.config

import com.sksamuel.hoplite.Masked

data class OAuthConfig(
    val tokenEndpoint: String,
    val clientId: String,
    val clientSecret: Masked,
    val scope: String,
    val grantType: String,
)
