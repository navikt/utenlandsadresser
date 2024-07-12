package no.nav.utenlandsadresser.sporingslogg.cleanup.kotest

import io.kotest.core.config.AbstractProjectConfig

object KotestProjectConfig : AbstractProjectConfig() {
    override val parallelism = Runtime.getRuntime().availableProcessors()
    override val autoScanEnabled = false
}
