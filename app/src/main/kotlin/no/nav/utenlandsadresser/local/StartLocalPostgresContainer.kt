package no.nav.utenlandsadresser.local

import com.sksamuel.hoplite.Masked
import no.nav.utenlandsadresser.config.UtenlandsadresserDatabaseConfig
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Driver

fun startLocalPostgresContainer(): UtenlandsadresserDatabaseConfig {
    val databaseName = "utenlandsadresser"
    val container =
        PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine")).apply {
            withDatabaseName(databaseName)
            withUsername("utenlandsadresser")
            withPassword("utenlandsadresser")
            start()
        }

    val port = container.firstMappedPort.toString()
    return UtenlandsadresserDatabaseConfig(
        username = container.username,
        password = Masked(container.password),
        driverClassName = "org.postgresql.Driver",
        jdbcUrl = container.jdbcUrl,
        host = container.host,
        port = port,
        url = "postgresql://${container.username}:${container.password}@${container.host}:$port/$databaseName",
    )
}
