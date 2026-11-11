package no.nav.utenlandsadresser.config

import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig

data class UtenlandsadresserConfig(
    val maskinporten: MaskinportenConfig,
    val utenlandsadresserDatabase: UtenlandsadresserDatabaseConfig,
    val oAuth: OAuthConfig,
    val pdl: PdlConfig,
    val registeroppslag: RegisteroppslagConfig,
    val utenlandsadresserBehandlingsnummer: String,
    val utenlandskIdBehandlingsnummer: String,
    val kafka: KafkaConfig,
    val unleash: UnleashConfig,
)
