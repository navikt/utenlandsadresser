package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresSporingsloggRepository
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

/**
 * Sette opp alle repositories som brukes av applikasjonen.
 *
 * @see Repositories
 */
fun configureRepositories(dataSource: DataSource): Repositories {
    val database = Database.connect(dataSource)
    val abonnementRepository = PostgresAbonnementRepository(database)
    val feedRepository = PostgresFeedRepository(database)
    val abonnementInitializer = PostgresAbonnementInitializer(abonnementRepository, feedRepository)
    val sporingslogg = PostgresSporingsloggRepository(database)
    val feedEventCreator = PostgresFeedEventCreator(feedRepository, abonnementRepository)

    return Repositories(
        abonnementRepository = abonnementRepository,
        abonnementInitializer = abonnementInitializer,
        feedRepository = feedRepository,
        sporingsloggRepository = sporingslogg,
        feedEventCreator = feedEventCreator,
    )
}
