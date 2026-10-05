package no.nav.utenlandsadresser.config

import no.nav.utenlandsadresser.felles.auth.OAuthConfig

data class UtenlandsadresserConfig(
    val maskinporten: MaskinportenConfig,
    val utenlandsadresserDatabase: UtenlandsadresserDatabaseConfig,
    val oAuth: OAuthConfig,
    val pdl: PdlConfig,
    val registeroppslag: RegisteroppslagConfig,
    val utenlandsadresserBehandlingsnummer: String,
    // TODO: Sendes ikke til PDL før eget behandlingsnummer for utenlandsk id er opprettet
    val utenlandskIdBehandlingsnummer: String? = null,
    val kafka: KafkaConfig,
    val unleash: UnleashConfig,
)
