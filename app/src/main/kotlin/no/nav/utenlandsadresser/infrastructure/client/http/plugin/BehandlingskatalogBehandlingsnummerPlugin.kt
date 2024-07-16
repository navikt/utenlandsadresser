package no.nav.utenlandsadresser.infrastructure.client.http.plugin

import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.createClientPlugin
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer

val BehandlingskatalogBehandlingsnummerHeaderPlugin =
    createClientPlugin("BehandlingskatalogBehandlingsnummerHeader", ::BehandlingsnummerHeaderConfig) {
        val behandlingskatalogBehandlingsnummer: BehandlingskatalogBehandlingsnummer =
            pluginConfig.behandlingskatalogBehandlingsnummer!!

        onRequest { request, _ ->
            request.headers.append("Behandlingsnummer", behandlingskatalogBehandlingsnummer.value)
        }
    }

fun HttpClient.configureBehandlingskatalogBehandlingsnummerHeader(
    behandlingskatalogBehandlingsnummer: BehandlingskatalogBehandlingsnummer,
): HttpClient =
    config {
        install(BehandlingskatalogBehandlingsnummerHeaderPlugin) {
            this.behandlingskatalogBehandlingsnummer = behandlingskatalogBehandlingsnummer
        }
    }

class BehandlingsnummerHeaderConfig {
    var behandlingskatalogBehandlingsnummer: BehandlingskatalogBehandlingsnummer? = null
}
