package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.Plugins
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import org.slf4j.LoggerFactory

/**
 * Sette opp alle tjenester som brukes av applikasjonen.
 *
 * @see Services
 */
fun setupServices(
    repositories: Repositories,
    clients: Clients,
    plugins: Plugins,
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
            plugins.meterRegistry.counter("utenlandsadresser_utleverte_utenlandsadresser_total"),
        )

    return Services(
        abonnementService = abonnementService,
        feedService = feedService,
    )
}
