package no.nav.utenlandsadresser.local

import com.sksamuel.hoplite.Masked
import no.nav.utenlandsadresser.config.UtenlandsadresserDatabaseConfig
import org.postgresql.Driver
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

fun startLocalPostgresContainer(): UtenlandsadresserDatabaseConfig {
    val container =
        PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine")).apply {
            withDatabaseName("utenlandsadresser")
            withUsername("utenlandsadresser")
            withPassword("utenlandsadresser")
            start()
        }

    return UtenlandsadresserDatabaseConfig(
        username = container.username,
        password = Masked(container.password),
        driverClassName = Driver::class.qualifiedName!!,
        jdbcUrl = container.jdbcUrl,
    )
}
