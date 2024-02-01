package no.nav.utenlandsadresser.plugin

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.utenlandsadresser.plugin.config.OAuthConfig
import java.util.concurrent.TimeUnit

private fun Application.configureOAuth(config: OAuthConfig, authenticationProviderName: String) {
    val jwkProvider = JwkProviderBuilder(config.issuer.value)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    authentication {
        jwt(authenticationProviderName) {
            verifier(jwkProvider, config.issuer.value) {
                acceptLeeway(3)
            }
            validate { credential ->
                val jwtScopes = credential.payload.getClaim("scope")?.asString()?.split(" ")

                if (jwtScopes?.any { it in config.scopes } == true) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
