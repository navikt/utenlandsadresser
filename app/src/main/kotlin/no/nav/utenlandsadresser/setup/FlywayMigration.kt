package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.config.UtenlandsadresserDatabaseConfig
import org.flywaydb.core.Flyway

/**
 * Kjører migrering av databasen.
 * Filene som brukes for migrering ligger under `resources/db/migration`.
 */
fun flywayMigration(config: UtenlandsadresserDatabaseConfig) {
    val flyway =
        Flyway
            .configure()
            .dataSource(config.jdbcUrl, config.username, config.password.value)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .load()

    flyway.migrate()
}
