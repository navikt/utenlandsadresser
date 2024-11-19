package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import org.slf4j.LoggerFactory

fun configureServices(
    repositories: Repositories,
    clients: Clients,
): Services {
    val abonnementService =
        AbonnementService(
            repositories.abonnementRepository,
            clients.regOppslagClient,
            repositories.abonnementInitializer,
        )
    val feedService =
        FeedService(
            repositories.feedRepository,
            clients.regOppslagClient,
            repositories.sporingsloggRepository,
            LoggerFactory.getLogger(FeedService::class.java),
        )

    return Services(
        abonnementService = abonnementService,
        feedService = feedService,
    )
}
