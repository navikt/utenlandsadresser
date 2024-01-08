package no.nav.utenlandsadresser.clients.http.pdl

import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returnsJson
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotest.extension.setupWiremockServer
import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery
import no.nav.utenlandsadresser.domain.BaseUrl
import no.nav.utenlandsadresser.routes.SchemaJson

class PdlHttpClientTest : WordSpec({
    val mockServer = setupWiremockServer()
    val baseUrl by lazy { mockServer.baseUrl() }
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    "querying the pdl client with a introspection query" should {
        val pdlClient = PdlHttpClient(httpClient, BaseUrl(baseUrl).getOrNull()!!)
        "return the primary query type" {
            mockServer.post {
                url equalTo "/graphql"
                // language=JSON
                body equalTo """{"query" : "{__schema {queryType {name}}}"}"""
            } returnsJson {
                // language=json
                body = """{"data": {"__schema": {"queryType": {"name": "Query"}}}}"""
            }

            // language=GraphQL
            val query = GraphQlQuery("""{__schema {queryType {name}}}""")
            val result = pdlClient.executeQuery(query, SchemaJson::class)

            result.data.schema.queryType.name shouldBe "Query"
        }
    }
})
