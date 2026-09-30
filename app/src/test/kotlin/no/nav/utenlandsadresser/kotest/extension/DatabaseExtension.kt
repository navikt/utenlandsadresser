package no.nav.utenlandsadresser.kotest.extension

import io.kotest.core.extensions.install
import io.kotest.core.spec.AbstractSpec
import io.kotest.extensions.testcontainers.TestContainerProjectExtension
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.locations.LocationParser
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection

private val sqlContainer = PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))

private lateinit var flyway: Flyway

fun AbstractSpec.setupDatabase(): R2dbcDatabase {
    install(
        TestContainerProjectExtension(
            container = sqlContainer,
            onStart = {
                flyway =
                    Flyway
                        .configure()
                        .locations(LocationParser.parseLocation("filesystem:src/main/resources/db/migration"))
                        .dataSource(sqlContainer.jdbcUrl, sqlContainer.username, sqlContainer.password)
                        .cleanDisabled(false)
                        .connectRetries(10)
                        .connectRetriesInterval(1)
                        .load()!!
            },
        ),
    )

    beforeTest {
        flyway.clean()
        flyway.migrate()
    }

    val r2dbcUrl =
        "r2dbc:postgresql://${sqlContainer.username}:${sqlContainer.password}@${sqlContainer.host}:${sqlContainer.firstMappedPort}/${sqlContainer.databaseName}"
    return R2dbcDatabase.connect(r2dbcUrl)
}

/**
 * Tømmer databasen og migrerer til [target]. Brukes til å teste en migrering mot data fra en tidligere versjon.
 * Krever at [setupDatabase] er kalt i samme spec.
 */
fun migrateDatabaseTo(target: String) {
    flyway.clean()
    Flyway
        .configure()
        .configuration(flyway.configuration)
        .target(target)
        .load()
        .migrate()
}

fun migrateDatabaseToLatest() {
    flyway.migrate()
}

fun <T> withJdbcConnection(block: (Connection) -> T): T = flyway.configuration.dataSource.connection.use(block)
