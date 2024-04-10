package no.nav.utenlandsadresser.plugin.maskinporten

import arrow.core.getOrElse
import arrow.core.raise.either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.domain.Scope

fun Route.protectWithScopes(scopes: Set<Scope>) {
    intercept(ApplicationCallPipeline.Call) {
        call.hasRequiredScopes(scopes).getOrElse {
            when (it) {
                MaskinportenScopeError.MissingPrincipal -> call.respond(
                    HttpStatusCode.Unauthorized,
                    "Missing JWT principal"
                )

                MaskinportenScopeError.MissingScopeClaim -> call.respond(
                    HttpStatusCode.Forbidden,
                    "Missing scope claim in JWT token"
                )

                MaskinportenScopeError.MissingRequiredScope -> call.respond(
                    HttpStatusCode.Forbidden,
                    "Missing required scope"
                )
            }
            return@intercept finish()
        }
    }
}

fun ApplicationCall.hasRequiredScopes(requiredScopes: Set<Scope>) = either {
    val principal = principal<JWTPrincipal>()
        ?: raise(MaskinportenScopeError.MissingPrincipal)

    val scopeClaim = principal.payload.getClaim("scope")
    if (scopeClaim.isMissing) {
        raise(MaskinportenScopeError.MissingScopeClaim)
    }
    val tokenScopes = scopeClaim.asString().split(" ").map { Scope(it) }.toSet()

    if (requiredScopes.none { tokenScopes.contains(it) }) {
        raise(MaskinportenScopeError.MissingRequiredScope)
    }
}

sealed class MaskinportenScopeError {
    data object MissingPrincipal : MaskinportenScopeError()
    data object MissingScopeClaim : MaskinportenScopeError()
    data object MissingRequiredScope : MaskinportenScopeError()
}