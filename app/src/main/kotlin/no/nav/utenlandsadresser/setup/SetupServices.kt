package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.Plugins
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.app.UtenlandskIdAbonnementService
import no.nav.utenlandsadresser.app.UtenlandskIdFeedService
import org.slf4j.LoggerFactory

/**
 * Sette opp alle tjenester som brukes av applikasjonen.
 *
 * @see Services
 */
private const val STOPPET_ABONNEMENT_COUNTER = "utenlandsadresser_feed_stoppet_abonnement_total"

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
            repositories.abonnementRepository,
            clients.regOppslagClient,
            repositories.sporingsloggRepository,
            LoggerFactory.getLogger(FeedService::class.java),
            plugins.meterRegistry.counter("utenlandsadresser_utleverte_utenlandsadresser_total"),
            plugins.meterRegistry.counter(STOPPET_ABONNEMENT_COUNTER, "feed", "postadresse"),
        )

    val utenlandskIdAbonnementService =
        UtenlandskIdAbonnementService(
            repositories.utenlandskIdAbonnementRepository,
            clients.hentUtenlandskIdClient,
            repositories.utenlandskIdAbonnementInitializer,
        )
    val utenlandskIdFeedService =
        UtenlandskIdFeedService(
            repositories.utenlandskIdFeedRepository,
            repositories.utenlandskIdAbonnementRepository,
            clients.hentUtenlandskIdClient,
            repositories.sporingsloggRepository,
            LoggerFactory.getLogger(UtenlandskIdFeedService::class.java),
            plugins.meterRegistry.counter("utenlandsadresser_utleverte_utenlandske_id_total"),
            plugins.meterRegistry.counter(STOPPET_ABONNEMENT_COUNTER, "feed", "utenlandskid"),
        )

    return Services(
        abonnementService = abonnementService,
        feedService = feedService,
        utenlandskIdAbonnementService = utenlandskIdAbonnementService,
        utenlandskIdFeedService = utenlandskIdFeedService,
    )
}
