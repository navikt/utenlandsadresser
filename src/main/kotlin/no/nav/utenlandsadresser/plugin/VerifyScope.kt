package no.nav.utenlandsadresser.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
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

        // Assign client_id to call attributes for later use
        call.assignClientIdToCallAttributes(decodedJwt)

        val claimedScopes = decodedJwt.getClaim("scope").asString()?.split(" ")
            ?: return@onCall call.respond(HttpStatusCode.Forbidden, "Missing scope claim in JWT token")

        if (claimedScopes.none { it == scope.value }) {
            return@onCall call.respond(
                HttpStatusCode.Forbidden,
                "Missing required scope ${scope.value}"
            )
        }
    }
}

val ClientIdKey = AttributeKey<String>("ClientId")

suspend fun ApplicationCall.assignClientIdToCallAttributes(decodedJwt: DecodedJWT) {
    val clientId = decodedJwt.getClaim("client_id")?.asString()
        ?: return respond(HttpStatusCode.Unauthorized, "Missing client_id claim in JWT token")

    attributes.put(ClientIdKey, clientId)
}
