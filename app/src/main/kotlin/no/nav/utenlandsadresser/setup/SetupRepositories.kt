package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresSporingsloggRepository
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

/**
 * Sette opp alle repositories som brukes av applikasjonen.
 *
 * @see Repositories
 */
context(config: UtenlandsadresserConfig)
fun setupRepositories(): Repositories {
    val database =
        R2dbcDatabase.connect(
            "r2dbc:${config.utenlandsadresserDatabase.url}",
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
