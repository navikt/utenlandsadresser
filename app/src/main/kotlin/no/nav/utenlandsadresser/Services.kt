package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.app.UtenlandskIdAbonnementService
import no.nav.utenlandsadresser.app.UtenlandskIdFeedService

data class Services(
    val abonnementService: AbonnementService,
    val feedService: FeedService,
    val utenlandskIdAbonnementService: UtenlandskIdAbonnementService,
    val utenlandskIdFeedService: UtenlandskIdFeedService,
)