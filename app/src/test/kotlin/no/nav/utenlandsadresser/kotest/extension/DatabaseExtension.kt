package no.nav.utenlandsadresser.kotest.extension

import io.kotest.core.extensions.install
import io.kotest.core.spec.DslDrivenSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.extensions.testcontainers.ContainerLifecycleMode
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.locations.LocationParser
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

private val sqlContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))

private lateinit var flyway: Flyway

fun DslDrivenSpec.setupDatabase(): Database {
    install(
        ContainerExtension(
            sqlContainer,
            mode = ContainerLifecycleMode.Project,
            afterStart = {
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
            beforeTest = {
                flyway.migrate()
            },
            afterTest = {
                flyway.clean()
            },
        ),
    )

    return Database.connect(
        url = sqlContainer.jdbcUrl,
        user = sqlContainer.username,
        password = sqlContainer.password,
        driver = org.postgresql.Driver::class.qualifiedName!!,
    )
}
