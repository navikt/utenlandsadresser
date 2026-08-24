package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.ConfigAlias
import com.sksamuel.hoplite.Masked

data class UtenlandsadresserDatabaseConfig(
    val username: String,
    val password: Masked,
    val host: String,
    val port: String,
    val url: Masked,
    @ConfigAlias("name")
    val databaseName: String,
) {
    val jdbcUrl: String = "jdbc:postgresql://$host:$port/$databaseName"
}
