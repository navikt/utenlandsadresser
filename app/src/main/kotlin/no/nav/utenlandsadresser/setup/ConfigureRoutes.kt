package no.nav.utenlandsadresser.setup

import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.EventConsumers
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.infrastructure.route.configureDevRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureLivenessRoute
import no.nav.utenlandsadresser.infrastructure.route.configurePostadresseRoutes
import no.nav.utenlandsadresser.infrastructure.route.configureReadinessRoute
import no.nav.utenlandsadresser.infrastructure.route.configureSporingsloggRoutes
import org.slf4j.LoggerFactory

fun Application.configureRoutes(
    services: Services,
    eventConsumers: EventConsumers,
    repositories: Repositories,
    appEnv: AppEnv,
    clients: Clients,
) {
    routing {
        configurePostadresseRoutes(
            services.abonnementService,
            services.feedService,
        )
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
                -> configureDevRoutes(clients.regOppslagClient, clients.maskinportenClient)

                AppEnv.PROD_GCP -> {}
            }
        }
        route("/api.json") {
            openApiSpec()
        }
        route("/docs/swagger") {
            swaggerUI("/api.json")
        }
    }
}
