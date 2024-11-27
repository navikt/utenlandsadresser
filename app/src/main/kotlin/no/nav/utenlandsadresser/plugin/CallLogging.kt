package no.nav.utenlandsadresser.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
        logger = LoggerFactory.getLogger("CallLogging")
        format { call ->
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val status = call.response.status()
            val processingTime =
                call.processingTimeMillis {
                    Clock.System.now().toEpochMilliseconds()
                }

            "$status - $method $path ${processingTime}ms"
        }
    }
}
