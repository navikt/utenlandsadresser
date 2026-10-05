package no.nav.utenlandsadresser.hent.utenlandsadresser.config

import no.nav.utenlandsadresser.felles.auth.OAuthConfig

data class HentUtenlandsadresserConfig(
    val pdlMottak: PdlMottakConfig,
    val oAuth: OAuthConfig,
    val utenlandsadresser: UtenlandsadresserConfig,
)
