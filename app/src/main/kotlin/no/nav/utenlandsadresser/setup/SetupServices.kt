package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.application.service.AbonnementService
import no.nav.utenlandsadresser.application.service.FeedService
import no.nav.utenlandsadresser.application.service.UtenlandskIdAbonnementService
import no.nav.utenlandsadresser.application.service.UtenlandskIdFeedService
import kotlin.time.Clock

/**
 * Sette opp alle tjenester som brukes av applikasjonen.
 *
 * @see Services
 */
context(clock: Clock)
fun setupServices(
    repositories: Repositories,
    clients: Clients,
): Services {
    val abonnementService =
        AbonnementService(
            repositories.abonnementRepository,
            clients.regOppslagClient,
            repositories.abonnementInitializer,
            clock,
        )
    val feedService =
        FeedService(
            repositories.feedRepository,
            repositories.abonnementRepository,
            clients.regOppslagClient,
            repositories.sporingsloggRepository,
            clock,
        )

    val utenlandskIdAbonnementService =
        UtenlandskIdAbonnementService(
            repositories.utenlandskIdAbonnementRepository,
            clients.hentUtenlandskIdClient,
            repositories.utenlandskIdAbonnementInitializer,
            clock,
        )
    val utenlandskIdFeedService =
        UtenlandskIdFeedService(
            repositories.utenlandskIdFeedRepository,
            repositories.utenlandskIdAbonnementRepository,
            clients.hentUtenlandskIdClient,
            repositories.sporingsloggRepository,
            clock,
        )

    return Services(
        abonnementService = abonnementService,
        feedService = feedService,
        utenlandskIdAbonnementService = utenlandskIdAbonnementService,
        utenlandskIdFeedService = utenlandskIdFeedService,
    )
}
