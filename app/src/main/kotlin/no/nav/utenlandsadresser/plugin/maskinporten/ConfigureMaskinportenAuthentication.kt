package no.nav.utenlandsadresser.plugin.maskinporten

import com.auth0.jwk.JwkProvider
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Scope

fun Application.configureMaskinportenAuthentication(
    issuer: Issuer,
    expectedScopes: Set<Scope>,
    jwkProvider: JwkProvider,
) {
    authentication {
        jwt("maskinporten") {
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

                if (expectedScopes.none { scopes.contains(it.value) }) {
                    return@validate null
                }

                JWTPrincipal(payload)
            }
        }
    }
}
