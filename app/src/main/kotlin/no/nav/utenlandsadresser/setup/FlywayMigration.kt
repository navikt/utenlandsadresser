package no.nav.utenlandsadresser.setup

import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun flywayMigration(dataSource: DataSource) {
    val flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .load()

    flyway.migrate()
}
