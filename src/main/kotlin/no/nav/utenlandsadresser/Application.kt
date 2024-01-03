package no.nav.utenlandsadresser

import arrow.core.getOrElse
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.utenlandsadresser.plugins.configureLogging
import no.nav.utenlandsadresser.plugins.configureMetrics
import no.nav.utenlandsadresser.plugins.configureRouting
import no.nav.utenlandsadresser.plugins.configureSerialization
import no.nav.utenlandsadresser.plugins.security.DevApiCredentials
import no.nav.utenlandsadresser.plugins.security.configureSecurity
import org.slf4j.LoggerFactory

fun main() {
    configureLogging(KtorEnv.getFromEnvVariable("KTOR_ENV"))
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("ConfigureApplication")
    val ktorEnv = KtorEnv.getFromEnvVariable("KTOR_ENV")

    val config = when (ktorEnv) {
        KtorEnv.LOCAL -> "application.conf"
        KtorEnv.DEV_GCP -> "application-dev-gcp.conf"
        KtorEnv.PROD_GCP -> "application-prod-gcp.conf"
    }.let { ConfigFactory.load(it) }

    println("Running in ${ktorEnv.name} environment")
    println("PDL URL: ${config.getString("pdl.url")}")

    val devApiCredentials = DevApiCredentials(
        name = System.getenv("DEV_API_USERNAME"),
        password = System.getenv("DEV_API_PASSWORD"),
    ).getOrElse { errors ->
        errors.forEach {
            logger.error(it.toLogMessage())
        }
        null
    }

    configureSecurity(
        devApiCredentials,
    )
    configureMetrics()
    configureSerialization()
    configureRouting()
}

private fun DevApiCredentials.Error.toLogMessage(): String = when (this) {
    DevApiCredentials.Error.NameMissing -> "Environment variable DEV_API_USERNAME not set"
    DevApiCredentials.Error.PasswordMissing -> "Environment variable DEV_API_PASSWORD not set"
}
