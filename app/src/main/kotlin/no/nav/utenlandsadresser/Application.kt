package no.nav.utenlandsadresser

import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.utenlandsadresser.adapter.outbound.metrics.MicrometerMetrikker
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.felles.AppEnv
import no.nav.utenlandsadresser.felles.logging.configureLogging
import no.nav.utenlandsadresser.setup.flywayMigration
import no.nav.utenlandsadresser.setup.launchBackgroundJobs
import no.nav.utenlandsadresser.setup.loadConfiguration
import no.nav.utenlandsadresser.setup.setupApplicationPlugins
import no.nav.utenlandsadresser.setup.setupClients
import no.nav.utenlandsadresser.setup.setupEventConsumers
import no.nav.utenlandsadresser.setup.setupFeatureToggles
import no.nav.utenlandsadresser.setup.setupRepositories
import no.nav.utenlandsadresser.setup.setupRoutes
import no.nav.utenlandsadresser.setup.setupServices
import kotlin.time.Clock

fun main() {
    configureLogging(AppEnv.getFromEnvVariable("APP_ENV"))
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

private fun Application.module() {
    val appEnv = AppEnv.getFromEnvVariable("APP_ENV")
    log.info("Starting application in $appEnv")

    val config: UtenlandsadresserConfig = loadConfiguration(appEnv)
    val clock: Clock = Clock.System

    context(appEnv, config, clock) {
        val plugins = setupApplicationPlugins()
        val metrikker = MicrometerMetrikker(plugins.meterRegistry)
        val featureToggles = setupFeatureToggles(plugins)
        flywayMigration()
        val repositories = setupRepositories()
        val clients = setupClients()
        val services = setupServices(repositories, clients)
        val eventConsumers = setupEventConsumers(services)
        launchBackgroundJobs(eventConsumers)
        setupRoutes(services, eventConsumers, clients, featureToggles, metrikker)
    }
}
