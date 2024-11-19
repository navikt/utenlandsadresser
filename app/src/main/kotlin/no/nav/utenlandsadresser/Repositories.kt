package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.app.SporingsloggRepository
import no.nav.utenlandsadresser.app.AbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.app.FeedRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator

data class Repositories(
    val abonnementRepository: AbonnementRepository,
    val abonnementInitializer: AbonnementInitializer,
    val feedRepository: FeedRepository,
    val sporingsloggRepository: SporingsloggRepository,
    val feedEventCreator: PostgresFeedEventCreator,
)