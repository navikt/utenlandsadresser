package no.nav.utenlandsadresser.setup

import io.ktor.server.application.Application
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.EventConsumers
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.adapter.inbound.web.configureDevRoutes
import no.nav.utenlandsadresser.adapter.inbound.web.configureLivenessRoute
import no.nav.utenlandsadresser.adapter.inbound.web.configurePersondataRoute
import no.nav.utenlandsadresser.adapter.inbound.web.configurePersondataV3Route
import no.nav.utenlandsadresser.adapter.inbound.web.configurePostadresseRoutes
import no.nav.utenlandsadresser.adapter.inbound.web.configureReadinessRoute
import no.nav.utenlandsadresser.adapter.inbound.web.configureSporingsloggRoutes
import no.nav.utenlandsadresser.adapter.inbound.web.configureUtenlandskIdRoutes
import no.nav.utenlandsadresser.adapter.inbound.web.plugin.configureOpenApi
import no.nav.utenlandsadresser.application.port.outbound.FeatureToggles
import no.nav.utenlandsadresser.application.port.outbound.Metrikker
import no.nav.utenlandsadresser.felles.AppEnv
import kotlin.time.Clock

/**
 * Sette opp alle routes som tilbys av applikasjonen.
 *
 * Routes under `/internal` er kun tilgjengelige internt.
 * Routes under `/internal/dev` er kun tilgjengelig i dev-miljøet.
 */
@OptIn(ExperimentalKtorApi::class)
context(appEnv: AppEnv, clock: Clock)
fun Application.setupRoutes(
    services: Services,
    eventConsumers: EventConsumers,
    repositories: Repositories,
    clients: Clients,
    featureToggles: FeatureToggles,
    metrikker: Metrikker,
) {
    routing {
        configurePostadresseRoutes(
            services.abonnementService,
            services.feedService,
            metrikker,
        )
        configureUtenlandskIdRoutes(
            services.utenlandskIdAbonnementService,
            services.utenlandskIdFeedService,
            metrikker,
            featureToggles,
        )
        configurePersondataRoute()
        configurePersondataV3Route()
        route("/internal") {
            configureLivenessRoute(
                healthChecks = listOf(eventConsumers.livshendelserConsumer),
            )
            configureReadinessRoute()
            configureSporingsloggRoutes(repositories.sporingsloggRepository, clock)
            when (appEnv) {
                AppEnv.LOCAL,
                AppEnv.DEV_GCP,
                -> {
                    configureDevRoutes(
                        clients.postadresseOppslag,
                        clients.maskinportenClient,
                        clients.utenlandskIdOppslag,
                    )
                }

                AppEnv.PROD_GCP -> {}
            }
        }.hide() // Skjuler /internal for OpenAPI dokumentasjonen
        configureOpenApi()
    }
}
