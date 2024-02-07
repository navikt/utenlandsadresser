package no.nav.utenlandsadresser.plugin

import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun configureFlyway(
    dataSource: DataSource
) {
    val flyway = Flyway.configure()
        .dataSource(dataSource)
        .load()

    flyway.migrate()
}