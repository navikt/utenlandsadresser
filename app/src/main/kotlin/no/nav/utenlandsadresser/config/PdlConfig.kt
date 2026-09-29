package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked

data class PdlConfig(
    val baseUrl: String,
    val scope: String,
    val behandlingsnummer: Masked,
)
