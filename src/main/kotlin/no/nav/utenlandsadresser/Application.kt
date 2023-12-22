package no.nav.utenlandsadresser

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery
import no.nav.utenlandsadresser.clients.http.pdl.PdlHttpClient
import no.nav.utenlandsadresser.plugins.*

fun main() {
    configureLogging()
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureMetrics()
    configureSerialization()
    configureRouting()

    withHttpClient { httpClient ->
        runBlocking {
            val pdlClient = PdlHttpClient(httpClient)
            // language=GraphQL
            val query = GraphQlQuery(
                """
                {
                    __schema {
                        queryType {
                            name
                        }
                    }
                } 
                """.trimIndent()
            )

            pdlClient.executeQuery(
                query, Unit::class
            )
        }
    }
}
