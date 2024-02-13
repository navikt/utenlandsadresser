package kotest.extension

import io.kotest.core.spec.DslDrivenSpec
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun DslDrivenSpec.setupDatabase(): Database {
    val dataSourceUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;"

    lateinit var flyway: Flyway

    beforeTest {
        flyway = Flyway.configure()
            .dataSource(dataSourceUrl, "", "")
            .cleanDisabled(false)
            .load()

        flyway.migrate()
    }

    afterTest {
        flyway.clean()
    }

    return Database.connect(dataSourceUrl, driver = "org.h2.Driver")
}