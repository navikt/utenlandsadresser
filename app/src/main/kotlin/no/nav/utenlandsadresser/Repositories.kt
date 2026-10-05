package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.application.port.outbound.AbonnementInitializer
import no.nav.utenlandsadresser.application.port.outbound.AbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.FeedRepository
import no.nav.utenlandsadresser.application.port.outbound.SporingsloggRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementInitializer
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdFeedRepository

data class Repositories(
    val abonnementRepository: AbonnementRepository,
    val abonnementInitializer: AbonnementInitializer,
    val feedRepository: FeedRepository,
    val sporingsloggRepository: SporingsloggRepository,
    val feedEventCreator: PostgresFeedEventCreator,
    val utenlandskIdAbonnementRepository: UtenlandskIdAbonnementRepository,
    val utenlandskIdAbonnementInitializer: UtenlandskIdAbonnementInitializer,
    val utenlandskIdFeedRepository: UtenlandskIdFeedRepository,
)