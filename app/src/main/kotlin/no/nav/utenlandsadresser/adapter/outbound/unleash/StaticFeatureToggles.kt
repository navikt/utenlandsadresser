package no.nav.utenlandsadresser.adapter.outbound.unleash

import no.nav.utenlandsadresser.application.port.outbound.FeatureToggles
import no.nav.utenlandsadresser.application.port.outbound.Toggle

/**
 * Faste toggler, brukt lokalt og i tester.
 */
class StaticFeatureToggles(
    private val enabled: Set<Toggle>,
) : FeatureToggles {
    override fun isEnabled(toggle: Toggle): Boolean = toggle in enabled

    companion object {
        fun allEnabled(): StaticFeatureToggles = StaticFeatureToggles(Toggle.entries.toSet())
    }
}
