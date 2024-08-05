package no.nav.utenlandsadresser

import com.sksamuel.hoplite.Masked
import no.nav.utenlandsadresser.config.UtenlandsadresserDatabaseConfig
import org.postgresql.Driver
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

fun startLocalPostgresContainer(): UtenlandsadresserDatabaseConfig {
    val container =
        PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:15-alpine")).apply {
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
