package no.nav.utenlandsadresser

import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.setup.flywayMigration
import no.nav.utenlandsadresser.setup.launchBackgroundJobs
import no.nav.utenlandsadresser.setup.loadConfiguration
import no.nav.utenlandsadresser.setup.setupApplicationPlugins
import no.nav.utenlandsadresser.setup.setupClients
import no.nav.utenlandsadresser.setup.setupDataSource
import no.nav.utenlandsadresser.setup.setupEventConsumers
import no.nav.utenlandsadresser.setup.setupRepositories
import no.nav.utenlandsadresser.setup.setupRoutes
import no.nav.utenlandsadresser.setup.setupServices
import javax.sql.DataSource

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

    context(appEnv, config) {
        val plugins = setupApplicationPlugins()
        val dataSource: DataSource = setupDataSource()
        flywayMigration(dataSource)

        val repositories = setupRepositories(dataSource)
        val clients = setupClients()
        val services = setupServices(repositories, clients, plugins)
        val eventConsumers = setupEventConsumers(repositories)
        launchBackgroundJobs(eventConsumers)
        setupRoutes(services, eventConsumers, repositories, clients)
    }
}
