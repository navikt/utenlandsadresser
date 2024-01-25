package no.nav.utenlandsadresser.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import no.nav.utenlandsadresser.domain.Scope

class VerifyScopeFromJwtConfig {
    var scope: Scope? = null
}

val VerifyScopeFromJwt = createRouteScopedPlugin(name = "VerifyScopeFromJwt", createConfiguration = ::VerifyScopeFromJwtConfig) {
    val scope = pluginConfig.scope
        ?: throw IllegalStateException("Scope must be configured")

    onCall { call ->
        val authHeader = call.request.headers["Authorization"]
            ?: return@onCall call.respond(HttpStatusCode.Unauthorized, "Missing Authorization header")

        val jwtToken = authHeader.removePrefix("Bearer ").trim()

        // No verification of JWT token is done here, as this is done by KrakenD
        val decodedJwt = try {
            JWT.decode(jwtToken)
        } catch (e: JWTDecodeException) {
            return@onCall call.respond(HttpStatusCode.Unauthorized, "Invalid JWT token")
        }

        val claimedScopes = decodedJwt.getClaim("scope").asString()?.split(" ")
            ?: return@onCall call.respond(HttpStatusCode.Unauthorized, "Missing scope claim in JWT token")

        if (claimedScopes.none { it == scope.value }) {
            return@onCall call.respond(
                HttpStatusCode.Forbidden,
                "Missing required scope ${scope.value}"
            )
        }
    }
}