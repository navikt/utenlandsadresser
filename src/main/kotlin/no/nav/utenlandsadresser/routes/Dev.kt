package no.nav.utenlandsadresser.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.clients.PdlClient
import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery

fun Route.configureDevRoutes(pdlClient: PdlClient) {
    route("/dev") {
        authenticate("basic-dev-auth", "form-dev-auth") {
            get("/hello") {
                call.respond(HttpStatusCode.OK, "Hello, world!")
            }
        }

        // Not exposed in production, protected in dev by Naisdevice
        post("/pdl") {
            // language=GraphQL
            val query = GraphQlQuery(call.receiveText())
            val response = pdlClient.executeQuery(query, String::class)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}

@Serializable
data class SchemaJson(
    val data: Data
)

@Serializable
data class Data(
    @SerialName("__schema") val schema: Schema
)

@Serializable
data class Schema(
    val queryType: QueryType
)

@Serializable
data class QueryType(
    val name: String
)
