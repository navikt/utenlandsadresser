package no.nav.utenlandsadresser

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.setup.configureApplicationPlugins
import no.nav.utenlandsadresser.setup.configureClients
import no.nav.utenlandsadresser.setup.configureDataSource
import no.nav.utenlandsadresser.setup.configureEventConsumers
import no.nav.utenlandsadresser.setup.configureRepositories
import no.nav.utenlandsadresser.setup.configureRoutes
import no.nav.utenlandsadresser.setup.configureServices
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
    configureApplicationPlugins(config)

    val dataSource: DataSource = configureDataSource(appEnv, config)

    flywayMigration(dataSource)

    val repositories = configureRepositories(dataSource)
    val clients = configureClients(config)
    val services = configureServices(repositories, clients)
    val eventConsumers = configureEventConsumers(appEnv, config, repositories)

    launchBackgroundJobs(eventConsumers)
    configureRoutes(services, eventConsumers, repositories, appEnv, clients)
}
