package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked

data class UtenlandsadresserDatabaseConfig(
    val username: String?,
    val password: Masked?,
    val driverClassName: String,
    val jdbcUrl: String,
)
