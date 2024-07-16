package no.nav.utenlandsadresser.plugin.maskinporten

import com.auth0.jwk.JwkProvider
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Scope

fun Application.configureMaskinportenAuthentication(
    configurationName: String,
    issuer: Issuer,
    requiredScopes: Set<Scope>,
    jwkProvider: JwkProvider,
) {
    authentication {
        jwt(configurationName) {
            realm = "Maskinporten"
            verifier(jwkProvider)
            validate { credential ->
                val payload = credential.payload

                if (payload.issuer != issuer.value) {
                    return@validate null
                }

                val scopes =
                    payload
                        .getClaim("scope")
                        .asString()
                        .split(" ")
                        .toSet()

                // Verify that the token contains all required scopes. It must contain ALL required scopes.
                if (requiredScopes.any { !scopes.contains(it.value) }) {
                    return@validate null
                }

                JWTPrincipal(payload)
            }
        }
    }
}
