package no.nav.utenlandsadresser.plugin.config

import no.nav.utenlandsadresser.domain.Domain

data class OAuthConfig(
    val issuer: Domain,
    val scopes: List<String>,
)