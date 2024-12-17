package no.nav.utenlandsadresser

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.setup.setupApplicationPlugins
import no.nav.utenlandsadresser.setup.setupClients
import no.nav.utenlandsadresser.setup.setupDataSource
import no.nav.utenlandsadresser.setup.setupEventConsumers
import no.nav.utenlandsadresser.setup.setupRepositories
import no.nav.utenlandsadresser.setup.setupRoutes
import no.nav.utenlandsadresser.setup.setupServices
import no.nav.utenlandsadresser.setup.flywayMigration
import no.nav.utenlandsadresser.setup.launchBackgroundJobs
import no.nav.utenlandsadresser.setup.loadConfiguration
import org.slf4j.LoggerFactory
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
    val logger = LoggerFactory.getLogger(this::class.java)
    val appEnv = AppEnv.getFromEnvVariable("APP_ENV")
    logger.info("Starting application in $appEnv")

    val config: UtenlandsadresserConfig = loadConfiguration(appEnv)
    setupApplicationPlugins(config)

    val dataSource: DataSource = setupDataSource(appEnv, config)

    flywayMigration(dataSource)

    val repositories = setupRepositories(dataSource)
    val clients = setupClients(config)
    val services = setupServices(repositories, clients)
    val eventConsumers = setupEventConsumers(appEnv, config, repositories)

    launchBackgroundJobs(eventConsumers)
    setupRoutes(services, eventConsumers, repositories, appEnv, clients)
}
