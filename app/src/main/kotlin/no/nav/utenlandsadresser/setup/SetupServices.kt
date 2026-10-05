package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.application.service.AbonnementService
import no.nav.utenlandsadresser.application.service.FeedService
import no.nav.utenlandsadresser.application.service.LivshendelseService
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
            clients.postadresseOppslag,
            repositories.abonnementOppretter,
            clock,
        )
    val feedService =
        FeedService(
            repositories.feedRepository,
            repositories.abonnementRepository,
            clients.postadresseOppslag,
            repositories.sporingsloggRepository,
            clock,
        )

    val utenlandskIdAbonnementService =
        UtenlandskIdAbonnementService(
            repositories.utenlandskIdAbonnementRepository,
            clients.utenlandskIdOppslag,
            repositories.utenlandskIdAbonnementOppretter,
            clock,
        )
    val utenlandskIdFeedService =
        UtenlandskIdFeedService(
            repositories.utenlandskIdFeedRepository,
            repositories.utenlandskIdAbonnementRepository,
            clients.utenlandskIdOppslag,
            repositories.sporingsloggRepository,
            clock,
        )

    val livshendelseService =
        LivshendelseService(
            repositories.abonnementRepository,
            repositories.feedRepository,
        )

    return Services(
        startAbonnement = abonnementService,
        stoppAbonnement = abonnementService,
        lesFeed = feedService,
        startUtenlandskIdAbonnement = utenlandskIdAbonnementService,
        stoppUtenlandskIdAbonnement = utenlandskIdAbonnementService,
        lesUtenlandskIdFeed = utenlandskIdFeedService,
        håndterLivshendelse = livshendelseService,
    )
}
