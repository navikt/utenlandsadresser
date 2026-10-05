package no.nav.utenlandsadresser.setup

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.micrometer.core.instrument.Gauge
import no.nav.utenlandsadresser.Plugins
import no.nav.utenlandsadresser.adapter.outbound.unleash.StaticFeatureToggles
import no.nav.utenlandsadresser.adapter.outbound.unleash.UnleashFeatureToggles
import no.nav.utenlandsadresser.application.port.outbound.FeatureToggles
import no.nav.utenlandsadresser.application.port.outbound.Toggle
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.felles.AppEnv

private const val FEATURE_TOGGLE_GAUGE = "utenlandsadresser_feature_toggle"

/**
 * Setter opp feature toggles. Lokalt er alle toggler på. I dev og prod leses de fra Unleash.
 *
 * Hver toggle eksponeres som en gauge (1 = på, 0 = av), så vi ser hvilken verdi podene faktisk bruker.
 */
context(appEnv: AppEnv, config: UtenlandsadresserConfig)
fun Application.setupFeatureToggles(plugins: Plugins): FeatureToggles {
    val featureToggles =
        when (appEnv) {
            AppEnv.LOCAL -> {
                StaticFeatureToggles.allEnabled()
            }

            AppEnv.DEV_GCP,
            AppEnv.PROD_GCP,
            -> {
                UnleashFeatureToggles
                    .create(
                        config = config.unleash,
                        appName = "utenlandsadresser",
                        instanceId = System.getenv("HOSTNAME") ?: "utenlandsadresser",
                    ).also { unleash -> monitor.subscribe(ApplicationStopped) { unleash.close() } }
            }
        }

    Toggle.entries.forEach { toggle ->
        Gauge
            .builder(FEATURE_TOGGLE_GAUGE) { if (featureToggles.isEnabled(toggle)) 1 else 0 }
            .tag("toggle", toggle.navn)
            .register(plugins.meterRegistry)
    }

    return featureToggles
}
