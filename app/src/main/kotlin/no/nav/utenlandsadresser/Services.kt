package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.application.service.AbonnementService
import no.nav.utenlandsadresser.application.service.FeedService
import no.nav.utenlandsadresser.application.service.UtenlandskIdAbonnementService
import no.nav.utenlandsadresser.application.service.UtenlandskIdFeedService

data class Services(
    val abonnementService: AbonnementService,
    val feedService: FeedService,
    val utenlandskIdAbonnementService: UtenlandskIdAbonnementService,
    val utenlandskIdFeedService: UtenlandskIdFeedService,
)