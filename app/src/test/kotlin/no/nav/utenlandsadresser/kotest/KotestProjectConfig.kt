package no.nav.utenlandsadresser.kotest

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.config.LogLevel
import org.slf4j.LoggerFactory

object KotestProjectConfig : AbstractProjectConfig() {
    override val parallelism = Runtime.getRuntime().availableProcessors()
    override val logLevel: LogLevel = LogLevel.Info

    override suspend fun beforeProject() {
        super.beforeProject()
        configureLogging()
    }

    private fun configureLogging() {
        val loggerContext =
            (LoggerFactory.getILoggerFactory() as LoggerContext).apply {
                // Reset logger to remove any automatic configuration
                reset()
            }

        // Encoder
        val patternLayoutEncoder =
            PatternLayoutEncoder().apply {
                pattern = "%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
                context = loggerContext
                start()
            }

        // Console Appender
        val consoleAppender =
            ConsoleAppender<ILoggingEvent>().apply {
                encoder = patternLayoutEncoder
                context = loggerContext
                start()
            }

        // Root Logger
        loggerContext.getLogger("ROOT").apply {
            level = Level.INFO
            addAppender(consoleAppender)
        }

        loggerContext.getLogger("org.testcontainers").apply {
            level = Level.ERROR
        }
    }
}
