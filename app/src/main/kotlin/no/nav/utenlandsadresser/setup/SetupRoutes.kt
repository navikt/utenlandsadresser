package no.nav.utenlandsadresser.setup

import io.ktor.server.application.Application
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.EventConsumers
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

/**
 * Sette opp alle routes som tilbys av applikasjonen.
 *
 * Routes under `/internal` er kun tilgjengelige internt.
 * Routes under `/internal/dev` er kun tilgjengelig i dev-miljøet.
 */
@OptIn(ExperimentalKtorApi::class)
context(appEnv: AppEnv)
fun Application.setupRoutes(
    services: Services,
    eventConsumers: EventConsumers,
    clients: Clients,
    featureToggles: FeatureToggles,
    metrikker: Metrikker,
) {
    routing {
        configurePostadresseRoutes(
            services.startAbonnement,
            services.stoppAbonnement,
            services.lesFeed,
            metrikker,
        )
        configureUtenlandskIdRoutes(
            services.startUtenlandskIdAbonnement,
            services.stoppUtenlandskIdAbonnement,
            services.lesUtenlandskIdFeed,
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
            configureSporingsloggRoutes(services.skrivSporingslogg, services.slettSporingslogg)
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
