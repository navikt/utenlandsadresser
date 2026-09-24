package no.nav.utenlandsadresser.setup

import io.ktor.server.application.Application
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.EventConsumers
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.infrastructure.route.configureDevRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureLivenessRoute
import no.nav.utenlandsadresser.infrastructure.route.configurePersondataRoute
import no.nav.utenlandsadresser.infrastructure.route.configurePersondataV3Route
import no.nav.utenlandsadresser.infrastructure.route.configurePostadresseRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureReadinessRoute
import no.nav.utenlandsadresser.infrastructure.route.configureSporingsloggRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureUtenlandskIdRoute
import no.nav.utenlandsadresser.plugin.configureOpenApi
import org.slf4j.LoggerFactory

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
    repositories: Repositories,
    clients: Clients,
) {
    routing {
        configurePostadresseRoutes(
            services.abonnementService,
            services.feedService,
        )
        configureUtenlandskIdRoute()
        configurePersondataRoute()
        configurePersondataV3Route()
        route("/internal") {
            configureLivenessRoute(
                logger = LoggerFactory.getLogger("LivenessRoute"),
                healthChecks = listOf(eventConsumers.livshendelserConsumer),
            )
            configureReadinessRoute()
            configureSporingsloggRoutes(repositories.sporingsloggRepository)
            when (appEnv) {
                AppEnv.LOCAL,
                AppEnv.DEV_GCP,
                -> {
                    configureDevRoutes(clients.regOppslagClient, clients.maskinportenClient)
                }

                AppEnv.PROD_GCP -> {}
            }
        }.hide() // Skjuler /internal for OpenAPI dokumentasjonen
        configureOpenApi()
    }
}
