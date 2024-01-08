package no.nav.utenlandsadresser.clients.http.pdl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.reflect.*
import no.nav.utenlandsadresser.clients.PdlClient
import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery
import no.nav.utenlandsadresser.clients.http.pdl.json.GraphQlQueryJson
import no.nav.utenlandsadresser.domain.BaseUrl
import kotlin.reflect.KClass

class PdlHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: BaseUrl,
) : PdlClient {
    override suspend fun <T : Any> executeQuery(query: GraphQlQuery, responseType: KClass<T>): T {
        return httpClient.post(Url("${baseUrl.value}/graphql")) {
            contentType(ContentType.Application.Json)
            setBody(GraphQlQueryJson(query))
        }.body(TypeInfo(responseType, responseType.java))
    }
}
