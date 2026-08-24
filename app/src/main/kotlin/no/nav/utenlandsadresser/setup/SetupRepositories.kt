package no.nav.utenlandsadresser.setup

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.config.UtenlandsadresserDatabaseConfig
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresSporingsloggRepository
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Sette opp alle repositories som brukes av applikasjonen.
 *
 * @see Repositories
 */
context(config: UtenlandsadresserDatabaseConfig)
fun setupRepositories(): Repositories {
    val connectionFactory =
        PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration
                .builder()
                .host(config.host)
                .port(config.port.toInt())
                .database(config.databaseName)
                .username(config.username)
                .password(config.password.value)
                .build(),
        )
    val connectionPoolConfiguration =
        ConnectionPoolConfiguration
            .builder(
                connectionFactory,
            ).initialSize(10)
            .maxSize(10)
            .maxIdleTime(30.minutes.toJavaDuration())
            .build()
    val connectionPool =
        ConnectionPool(
            connectionPoolConfiguration,
        )
    val database =
        R2dbcDatabase.connect(
            connectionFactory = connectionPool,
            databaseConfig =
                R2dbcDatabaseConfig {
                    explicitDialect = PostgreSQLDialect()
                },
        )

    val abonnementRepository = PostgresAbonnementRepository(database)
    val feedRepository = PostgresFeedRepository(database)
    val abonnementInitializer = PostgresAbonnementInitializer(abonnementRepository, feedRepository, database)
    val sporingslogg = PostgresSporingsloggRepository(database)
    val feedEventCreator = PostgresFeedEventCreator(feedRepository, abonnementRepository, database)

    return Repositories(
        abonnementRepository = abonnementRepository,
        abonnementInitializer = abonnementInitializer,
        feedRepository = feedRepository,
        sporingsloggRepository = sporingslogg,
        feedEventCreator = feedEventCreator,
    )
}
