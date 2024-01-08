package no.nav.utenlandsadresser.clients.http.plugins.config

import io.ktor.client.*
import no.nav.utenlandsadresser.plugins.config.OAuthConfig

class BearerAuthConfig {
    var oAuthConfig: OAuthConfig? = null
    var tokenClient: HttpClient? = null
}