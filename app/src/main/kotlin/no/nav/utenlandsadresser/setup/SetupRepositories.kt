package no.nav.utenlandsadresser.setup

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.PostgresAbonnementOppretter
import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.PostgresAbonnementRepository
import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.PostgresFeedRepository
import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.PostgresSporingsloggRepository
import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.PostgresUtenlandskIdAbonnementOppretter
import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.PostgresUtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.PostgresUtenlandskIdFeedRepository
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
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
    val abonnementOppretter = PostgresAbonnementOppretter(abonnementRepository, feedRepository, database)
    val sporingslogg = PostgresSporingsloggRepository(database, clock)
    val utenlandskIdAbonnementRepository = PostgresUtenlandskIdAbonnementRepository(database)
    val utenlandskIdFeedRepository = PostgresUtenlandskIdFeedRepository(database, clock)
    val utenlandskIdAbonnementOppretter =
        PostgresUtenlandskIdAbonnementOppretter(utenlandskIdAbonnementRepository, utenlandskIdFeedRepository, database)

    return Repositories(
        abonnementRepository = abonnementRepository,
        abonnementOppretter = abonnementOppretter,
        feedRepository = feedRepository,
        sporingsloggRepository = sporingslogg,
        utenlandskIdAbonnementRepository = utenlandskIdAbonnementRepository,
        utenlandskIdAbonnementOppretter = utenlandskIdAbonnementOppretter,
        utenlandskIdFeedRepository = utenlandskIdFeedRepository,
    )
}
