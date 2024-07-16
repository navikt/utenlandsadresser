package no.nav.utenlandsadresser.plugin.maskinporten

import com.auth0.jwk.JwkProvider
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Scope

/**
 * Setter opp Maskinporten autentisering og autorisering. I utganspunktet valideres kun issuer og scope.
 *
 * @param configurationName Navn på konfigurasjonen som brukes for å autentisere tokenet. Brukes i routes for å referere til konfigurasjonen. Eksempel: [no.nav.utenlandsadresser.infrastructure.route.configurePostadresseRoutes]
 * @param issuer Hvem som har utstedt tokenet. Skal alltid være Maskinporten.
 * @param requiredScopes Hvilke scopes som må være inkludert i tokenet. Om ikke alle scopes er inkludert i tokenet så vil tokenet bli avvist.
 * @param jwkProvider JWK-provider som brukes for å validere signaturen på tokenet.
 * @param jwtConfigBlock Block som kan legge på ekstra konfigurasjon eller overskrive konfigurasjon.
 * @param jwtValidationBlock Block som legger på ekstra validering av token. Returnerer true hvis token er gyldig.
 */
fun Application.configureMaskinportenAuthentication(
    configurationName: String,
    issuer: Issuer,
    requiredScopes: Set<Scope>,
    jwkProvider: JwkProvider,
    jwtConfigBlock: JWTAuthenticationProvider.Config.() -> Unit = {},
    jwtValidationBlock: ApplicationCall.(JWTCredential) -> Boolean = { true },
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

                if (!jwtValidationBlock(credential)) {
                    return@validate null
                }

                JWTPrincipal(payload)
            }
            jwtConfigBlock()
        }
    }
}
