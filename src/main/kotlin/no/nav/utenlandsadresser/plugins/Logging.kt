package no.nav.utenlandsadresser.plugins

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.JsonEncoder
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import no.nav.utenlandsadresser.KtorEnv
import org.slf4j.LoggerFactory

fun configureLogging(env: KtorEnv) {
    val loggerContext = (LoggerFactory.getILoggerFactory() as LoggerContext).apply {
        // Reset logger to remove any automatic configuration
        reset()
    }

    // Encoder
    val patternLayoutEncoder = when (env) {
        KtorEnv.LOCAL -> PatternLayoutEncoder().apply {
            pattern = "%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
            context = loggerContext
            start()
        }

        KtorEnv.DEV_GCP, KtorEnv.PROD_GCP -> JsonEncoder().apply {
            context = loggerContext
            start()
        }
    }

    // Console Appender
    val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
        encoder = patternLayoutEncoder
        context = loggerContext
        start()
    }

    // Root Logger
    loggerContext.getLogger("ROOT").apply {
        level = Level.INFO
        addAppender(consoleAppender)
    }

    // Specific Loggers
    loggerContext.getLogger("io.netty").level = Level.WARN
}
