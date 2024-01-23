package no.nav.utenlandsadresser.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.Logger

fun Route.configurePostadresseRoutes(logger: Logger) {
    route("/postadresse") {
        route("/abonnement") {
            post("/start") {
                val authHeader = call.request.headers["Authorization"]
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Authorization header")

                val jwtToken = authHeader.removePrefix("Bearer ").trim()

                // No verification of JWT token is done here, as this is done by KrakenD
                val decodedJwt = try {
                    JWT.decode(jwtToken)
                } catch (e: JWTDecodeException) {
                    return@post call.respond(HttpStatusCode.Unauthorized, "Invalid JWT token")
                }

                logger.info("Scopes from JWT: ${decodedJwt.getClaim("scope").asString()}")

                return@post call.respond(HttpStatusCode.NotImplemented, "Not implemented yet")
            }
        }
    }
}
