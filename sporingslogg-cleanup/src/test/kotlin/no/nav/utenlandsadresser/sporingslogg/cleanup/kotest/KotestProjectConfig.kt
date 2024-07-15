package no.nav.utenlandsadresser.sporingslogg.cleanup.kotest

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.config.LogLevel

object KotestProjectConfig : AbstractProjectConfig() {
    override val parallelism = Runtime.getRuntime().availableProcessors()
    override val logLevel: LogLevel = LogLevel.Info
}
