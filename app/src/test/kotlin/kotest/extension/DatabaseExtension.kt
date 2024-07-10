package kotest.extension

import io.kotest.core.extensions.install
import io.kotest.core.spec.DslDrivenSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.extensions.testcontainers.ContainerLifecycleMode
import org.flywaydb.core.Flyway
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
                flyway = Flyway.configure()
                    .dataSource(sqlContainer.jdbcUrl, sqlContainer.username, sqlContainer.password)
                    .cleanDisabled(false)
                    .load()!!
            },
            beforeTest = {
                flyway.migrate()
            },
            afterTest = {
                flyway.clean()
            }
        )
    )

    return Database.connect(
        url = sqlContainer.jdbcUrl,
        user = sqlContainer.username,
        password = sqlContainer.password,
        driver = org.postgresql.Driver::class.qualifiedName!!
    )
}