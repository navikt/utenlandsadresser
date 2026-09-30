package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked

data class MaskinportenConfig(
    val clientId: String,
    val clientJwk: Masked,
    // Scopes som appen selv henter token for. Brukes av dev-endepunktet for Maskinporten-token.
    val scopes: String,
    // Scopes som kreves av konsumentene for å kalle API-ene
    val postadresseScope: String,
    val utenlandskIdScope: String,
    val consumers: List<String>,
    val wellKnownUrl: String,
    val issuer: String,
    val tokenEndpoint: String,
    val jwksUri: String,
)
