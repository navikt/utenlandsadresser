package no.nav.utenlandsadresser

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.utenlandsadresser.plugins.configureLogging
import no.nav.utenlandsadresser.plugins.configureMetrics
import no.nav.utenlandsadresser.plugins.configureRouting
import no.nav.utenlandsadresser.plugins.configureSerialization

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
    val ktorEnv = KtorEnv.getFromEnvVariable("KTOR_ENV")

    val config = when (ktorEnv) {
        KtorEnv.LOCAL -> "application.conf"
        KtorEnv.DEV_GCP -> "application-dev-gcp.conf"
        KtorEnv.PROD_GCP -> "application-prod-gcp.conf"
    }.let { ConfigFactory.load(it) }

    println(config.getString("pdl.url"))

    configureMetrics()
    configureSerialization()
    configureRouting()
}
