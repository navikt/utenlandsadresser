package no.nav.utenlandsadresser.clients.http.plugins

import io.ktor.client.*
import io.ktor.client.plugins.api.*
import no.nav.utenlandsadresser.clients.http.plugins.config.BehandlingsnummerHeaderConfig
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer


val BehandlingskatalogBehandlingsnummerHeader =
    createClientPlugin("BehandlingskatalogBehandlingsnummerHeader", ::BehandlingsnummerHeaderConfig) {
        val behandlingskatalogBehandlingsnummer: BehandlingskatalogBehandlingsnummer =
            pluginConfig.behandlingskatalogBehandlingsnummer!!

        onRequest { request, _ ->
            request.headers.append("Behandlingsnummer", behandlingskatalogBehandlingsnummer.value)
        }
    }

fun HttpClient.configureBehandlingskatalogBehandlingsnummerHeader(behandlingskatalogBehandlingsnummer: BehandlingskatalogBehandlingsnummer): HttpClient =
    config {
        install(BehandlingskatalogBehandlingsnummerHeader) {
            this.behandlingskatalogBehandlingsnummer = behandlingskatalogBehandlingsnummer
        }
    }
