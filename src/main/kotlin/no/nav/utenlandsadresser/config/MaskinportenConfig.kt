package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked

data class MaskinportenConfig(
    val clientId: String,
    val clientJwk: Masked,
    val scopes: String,
    val wellKnownUrl: String,
    val issuer: String,
    val tokenEndpoint: String,
    val jwksUri: String,
)
