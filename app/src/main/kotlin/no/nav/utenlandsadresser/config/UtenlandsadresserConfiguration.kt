package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig

data class UtenlandsadresserConfiguration(
    val maskinporten: MaskinportenConfig,
    val utenlandsadresserDatabase: UtenlandsadresserDatabaseConfig,
    val oAuth: OAuthConfig,
    val registeroppslag: RegisteroppslagConfig,
    val behandlingskatalogBehandlingsnummer: Masked,
    val kafka: KafkaConfig,
)
