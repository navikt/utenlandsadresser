package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig
import no.nav.utenlandsadresser.plugin.config.BasicAuthConfig

data class UtenlandsadresserConfig(
    val maskinporten: MaskinportenConfig,
    val utenlandsadresserDatabase: UtenlandsadresserDatabaseConfig,
    val oAuth: OAuthConfig,
    val registeroppslag: RegisteroppslagConfig,
    val basicAuth: BasicAuthConfig,
    val behandlingskatalogBehandlingsnummer: Masked,
    val kafka: KafkaConfig,
)
