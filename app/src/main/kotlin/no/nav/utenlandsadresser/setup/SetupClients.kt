package no.nav.utenlandsadresser.setup

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.http.Url
import no.nav.utenlandsadresser.Clients
import no.nav.utenlandsadresser.adapter.outbound.maskinporten.MaskinportenHttpClient
import no.nav.utenlandsadresser.adapter.outbound.pdl.PdlGraphQLClient
import no.nav.utenlandsadresser.adapter.outbound.registeroppslag.RegisteroppslagHttpClient
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.felles.auth.Scope
import no.nav.utenlandsadresser.felles.http.createAuthHttpClient
import no.nav.utenlandsadresser.felles.http.createHttpClient
import java.net.URI

/**
 * Sette opp alle klienter som brukes av applikasjonen.
 *
 * @see Clients
 */
context(config: UtenlandsadresserConfig)
fun setupClients(): Clients {
    val regOppslagClient =
        RegisteroppslagHttpClient(
            httpClient =
                createAuthHttpClient(
                    oAuthConfig = config.oAuth,
                    scopes = listOf(Scope(config.registeroppslag.scope)),
                ),
            baseUrl = Url(config.registeroppslag.baseUrl),
            behandlingsnummer = BehandlingskatalogBehandlingsnummer(config.utenlandsadresserBehandlingsnummer),
        )

    val maskinportenClient =
        MaskinportenHttpClient(
            maskinportenConfig = config.maskinporten,
            httpClient = createHttpClient(),
        )

    val pdlClient =
        PdlGraphQLClient(
            graphQLClient =
                GraphQLKtorClient(
                    url = URI.create(config.pdl.baseUrl).toURL(),
                    httpClient =
                        createAuthHttpClient(
                            oAuthConfig = config.oAuth,
                            scopes = listOf(Scope(config.pdl.scope)),
                        ),
                ),
            behandlingsnummer = config.utenlandskIdBehandlingsnummer?.let(::BehandlingskatalogBehandlingsnummer),
        )

    return Clients(
        regOppslagClient = regOppslagClient,
        maskinportenClient = maskinportenClient,
        hentUtenlandskIdClient = pdlClient,
    )
}
