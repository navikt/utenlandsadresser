package no.nav.utenlandsadresser.clients.http.pdl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.reflect.*
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.clients.PdlClient
import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class PdlHttpClient(
    private val httpClient: HttpClient,
) : PdlClient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun <T : Any> executeQuery(query: GraphQlQuery, responseType: KClass<T>): T {
        return httpClient.post(Url("https://pdl-playground.dev.intern.nav.no/graphql")) {
            contentType(ContentType.Application.Json)
            setBody(GraphQlQueryJson(query))
        }.also {
            logger.info("Request: $it")
        }.body(TypeInfo(responseType, responseType.java))
    }
}

@Serializable
data class GraphQlQueryJson(
    val query: GraphQlQuery,
)