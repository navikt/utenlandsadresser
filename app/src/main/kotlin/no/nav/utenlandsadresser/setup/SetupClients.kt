package no.nav.utenlandsadresser.setup

import io.ktor.http.Url
import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.client.http.createAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.createHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.maskinporten.MaskinportenHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.RegisteroppslagHttpClient

/**
 * Sette opp alle klienter som brukes av applikasjonen.
 *
 * @see Clients
 */
fun setupClients(config: UtenlandsadresserConfig): Clients {
    val regOppslagClient =
        RegisteroppslagHttpClient(
            httpClient =
                createAuthHttpClient(
                    oAuthConfig = config.oAuth,
                    scopes = listOf(Scope(config.registeroppslag.scope)),
                ),
            baseUrl = Url(config.registeroppslag.baseUrl),
            behandlingsnummer = BehandlingskatalogBehandlingsnummer(config.behandlingskatalogBehandlingsnummer.value),
        )

    val maskinportenClient =
        MaskinportenHttpClient(
            maskinportenConfig = config.maskinporten,
            httpClient = createHttpClient(),
        )

    return Clients(
        regOppslagClient = regOppslagClient,
        maskinportenClient = maskinportenClient,
    )
}
