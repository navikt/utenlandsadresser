package no.nav.utenlandsadresser.config

/**
 * Tilkobling til teamets Unleash-instans. Verdiene kommer fra secreten som `ApiToken`-ressursen lager.
 *
 * @property apiUrl Adressen til Unleash uten `/api`.
 */
data class UnleashConfig(
    val apiUrl: String,
    val apiToken: String,
    val environment: String,
)
