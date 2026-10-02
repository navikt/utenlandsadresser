package no.nav.utenlandsadresser.setup

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresSporingsloggRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresUtenlandskIdAbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresUtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresUtenlandskIdFeedRepository
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Sette opp alle repositories som brukes av applikasjonen.
 *
 * @see Repositories
 */
context(config: UtenlandsadresserConfig, clock: Clock)
fun setupRepositories(): Repositories {
    val databaseConfig = config.utenlandsadresserDatabase
    val connectionFactory =
        PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration
                .builder()
                .host(databaseConfig.host)
                .port(databaseConfig.port.toInt())
                .database(databaseConfig.databaseName)
                .username(databaseConfig.username)
                .password(databaseConfig.password.value)
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
    val feedRepository = PostgresFeedRepository(database, clock)
    val abonnementInitializer = PostgresAbonnementInitializer(abonnementRepository, feedRepository, database)
    val sporingslogg = PostgresSporingsloggRepository(database, clock)
    val feedEventCreator = PostgresFeedEventCreator(feedRepository, abonnementRepository, database)
    val utenlandskIdAbonnementRepository = PostgresUtenlandskIdAbonnementRepository(database)
    val utenlandskIdFeedRepository = PostgresUtenlandskIdFeedRepository(database, clock)
    val utenlandskIdAbonnementInitializer =
        PostgresUtenlandskIdAbonnementInitializer(utenlandskIdAbonnementRepository, utenlandskIdFeedRepository, database)

    return Repositories(
        abonnementRepository = abonnementRepository,
        abonnementInitializer = abonnementInitializer,
        feedRepository = feedRepository,
        sporingsloggRepository = sporingslogg,
        feedEventCreator = feedEventCreator,
        utenlandskIdAbonnementRepository = utenlandskIdAbonnementRepository,
        utenlandskIdAbonnementInitializer = utenlandskIdAbonnementInitializer,
        utenlandskIdFeedRepository = utenlandskIdFeedRepository,
    )
}
