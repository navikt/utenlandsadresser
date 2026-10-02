package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import org.flywaydb.core.Flyway

/**
 * Kjører migrering av databasen.
 * Filene som brukes for migrering ligger under `resources/db/migration`.
 */
context(config: UtenlandsadresserConfig)
fun flywayMigration() {
    val databaseConfig = config.utenlandsadresserDatabase
    val flyway =
        Flyway
            .configure()
            .dataSource(databaseConfig.jdbcUrl, databaseConfig.username, databaseConfig.password.value)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .load()

    flyway.migrate()
}
