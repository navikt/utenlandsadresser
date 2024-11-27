package no.nav.utenlandsadresser.hent.utenlandsadresser.config

import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig

data class HentUtenlandsadresserConfig(
    val pdlMottak: PdlMottakConfig,
    val oAuth: OAuthConfig,
    val utenlandsadresser: UtenlandsadresserConfig,
)
