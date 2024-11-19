package no.nav.utenlandsadresser.setup

import io.ktor.http.Url
import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.config.UtenlandsadresserConfiguration
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.client.http.configureAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.configureHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.maskinporten.MaskinportenHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.RegisteroppslagHttpClient

fun configureClients(config: UtenlandsadresserConfiguration): Clients {
    val regoppslagAuthHttpClient =
        configureAuthHttpClient(
            config.oAuth,
            listOf(Scope(config.registeroppslag.scope)),
        )

    val regOppslagClient =
        RegisteroppslagHttpClient(
            regoppslagAuthHttpClient,
            Url(config.registeroppslag.baseUrl),
            BehandlingskatalogBehandlingsnummer(config.behandlingskatalogBehandlingsnummer.value),
        )

    val httpClient = configureHttpClient()
    val maskinportenClient =
        MaskinportenHttpClient(
            config.maskinporten,
            httpClient,
        )

    return Clients(
        regOppslagClient = regOppslagClient,
        maskinportenClient = maskinportenClient,
    )
}
