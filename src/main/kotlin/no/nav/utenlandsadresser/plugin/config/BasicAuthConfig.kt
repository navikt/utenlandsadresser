package no.nav.utenlandsadresser.plugin.config

import com.sksamuel.hoplite.Masked

data class BasicAuthConfig(
    val username: String,
    val password: Masked,
)