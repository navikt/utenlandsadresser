package no.nav.utenlandsadresser

import io.micrometer.core.instrument.MeterRegistry

data class Plugins(
    val meterRegistry: MeterRegistry,
)
