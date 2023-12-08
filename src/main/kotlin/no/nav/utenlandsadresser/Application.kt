package no.nav.utenlandsadresser

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.utenlandsadresser.plugins.*

fun main() {
    configureLogging()
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // TODO: Add confugration for security
    configureSecurity()
    configureMetrics()
    configureDatabases()
    configureSerialization()
    configureRouting()
    // TODO: Add configuration for OpenAPI
    //configureHTTP()
}
