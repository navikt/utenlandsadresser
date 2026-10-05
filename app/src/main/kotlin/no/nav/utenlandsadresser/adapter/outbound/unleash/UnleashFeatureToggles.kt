package no.nav.utenlandsadresser.adapter.outbound.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig as UnleashClientConfig
import no.nav.utenlandsadresser.application.port.outbound.FeatureToggles
import no.nav.utenlandsadresser.application.port.outbound.Toggle
import no.nav.utenlandsadresser.config.UnleashConfig

/**
 * Leser toggler fra Unleash. SDK-en henter togglene i bakgrunnen og holder dem i minnet, så [isEnabled] gjør ingen nettverkskall.
 *
 * Før første vellykkede henting er alle toggler av. Feiler hentingen senere, brukes siste kjente verdi.
 */
class UnleashFeatureToggles(
    private val unleash: Unleash,
) : FeatureToggles,
    AutoCloseable {
    override fun isEnabled(toggle: Toggle): Boolean = unleash.isEnabled(toggle.navn, false)

    override fun close() {
        unleash.shutdown()
    }

    companion object {
        fun create(
            config: UnleashConfig,
            appName: String,
            instanceId: String,
        ): UnleashFeatureToggles =
            UnleashFeatureToggles(
                DefaultUnleash(
                    UnleashClientConfig
                        .builder()
                        .appName(appName)
                        .instanceId(instanceId)
                        .unleashAPI("${config.apiUrl}/api")
                        .apiKey(config.apiToken)
                        .synchronousFetchOnInitialisation(false)
                        .build(),
                ),
            )
    }
}
