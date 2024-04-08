package no.nav.utenlandsadresser.plugin

import com.auth0.jwk.JwkProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Scope

fun Application.configureMaskinporten(
    issuer: Issuer,
    expectedScopes: Set<Scope>,
    jwkProvider: JwkProvider
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

                val scopes = payload.getClaim("scope").asString().split(" ").toSet()

                if (expectedScopes.none { scopes.contains(it.value) }) {
                    return@validate null
                }

                JWTPrincipal(credential.payload)
            }
        }
    }
}