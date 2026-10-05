package no.nav.utenlandsadresser.adapter.inbound.web.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respond

class FeatureToggleGateConfig {
    lateinit var erTilgjengelig: () -> Boolean
}

/**
 * Svarer 404 på alle kall under ruten når [FeatureToggleGateConfig.erTilgjengelig] svarer `false`.
 *
 * Installeres på ruten *utenfor* `authenticate { }`. Plugin-en kjører da i `Plugins`-fasen, før autentiseringen,
 * så den som kaller ikke kan skille en avslått funksjon fra et endepunkt som ikke finnes.
 * Tilgjengeligheten sjekkes ved hvert kall.
 */
val FeatureToggleGate =
    createRouteScopedPlugin("FeatureToggleGate", ::FeatureToggleGateConfig) {
        val erTilgjengelig = pluginConfig.erTilgjengelig
        onCall { call ->
            if (!erTilgjengelig()) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
