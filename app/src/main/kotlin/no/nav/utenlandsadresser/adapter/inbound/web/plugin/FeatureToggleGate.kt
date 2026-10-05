package no.nav.utenlandsadresser.adapter.inbound.web.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respond
import no.nav.utenlandsadresser.application.port.outbound.FeatureToggles
import no.nav.utenlandsadresser.application.port.outbound.Toggle

class FeatureToggleGateConfig {
    lateinit var featureToggles: FeatureToggles
    lateinit var toggle: Toggle
}

/**
 * Svarer 404 på alle kall under ruten når [FeatureToggleGateConfig.toggle] er av.
 *
 * Installeres på ruten *utenfor* `authenticate { }`. Plugin-en kjører da i `Plugins`-fasen, før autentiseringen,
 * så den som kaller ikke kan skille en avslått funksjon fra et endepunkt som ikke finnes.
 * Togglen sjekkes ved hvert kall.
 */
val FeatureToggleGate =
    createRouteScopedPlugin("FeatureToggleGate", ::FeatureToggleGateConfig) {
        val featureToggles = pluginConfig.featureToggles
        val toggle = pluginConfig.toggle
        onCall { call ->
            if (!featureToggles.isEnabled(toggle)) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
