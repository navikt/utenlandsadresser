package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService

data class Services(
    val abonnementService: AbonnementService,
    val feedService: FeedService,
)