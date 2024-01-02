package no.nav.utenlandsadresser.clients.http.pdl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.reflect.*
import no.nav.utenlandsadresser.clients.PdlClient
import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery
import no.nav.utenlandsadresser.clients.http.pdl.json.GraphQlQueryJson
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class PdlHttpClient(
    private val httpClient: HttpClient,
    private val config: PdlHttpeClientConfig,
) : PdlClient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun <T : Any> executeQuery(query: GraphQlQuery, responseType: KClass<T>): T {
        return httpClient.post(Url("${config.url}/graphql")) {
            contentType(ContentType.Application.Json)
            setBody(GraphQlQueryJson(query))
        }.also {
            logger.info("Request: $it")
        }.body(TypeInfo(responseType, responseType.java))
    }
}
