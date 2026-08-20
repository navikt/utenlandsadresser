package no.nav.utenlandsadresser.kotest.extension

import io.kotest.core.extensions.install
import io.kotest.core.spec.AbstractSpec
import io.kotest.extensions.testcontainers.TestContainerProjectExtension
import no.nav.utenlandsadresser.infrastructure.persistence.r2dbcUrl
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.locations.LocationParser
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

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
        r2dbcUrl(
            driver = "postgresql",
            user = sqlContainer.username,
            password = sqlContainer.password,
            host = sqlContainer.host,
            port = sqlContainer.firstMappedPort.toString(),
            path = sqlContainer.databaseName,
        )
    return R2dbcDatabase.connect(r2dbcUrl)
}
