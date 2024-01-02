package no.nav.utenlandsadresser

import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.utenlandsadresser.clients.graphql.GraphQlQuery
import no.nav.utenlandsadresser.clients.http.pdl.PdlHttpClient
import no.nav.utenlandsadresser.clients.http.pdl.PdlHttpeClientConfig
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
    val ktorEnv = KtorEnv.fromEnvVariable(
        System.getenv("KTOR_ENV"),
    )

    val config = when (ktorEnv) {
        KtorEnv.LOCAL -> "application.conf"
        KtorEnv.DEV_GCP -> "application-dev-gcp.conf"
        KtorEnv.PROD_GCP -> "application-prod-gcp.conf"
    }.let { ConfigFactory.load(it) }

    println(config.getString("pdl.url"))

    configureMetrics()
    configureSerialization()
    configureRouting()

    withHttpClient { httpClient ->
        runBlocking {
            val pdlClientConfig = PdlHttpeClientConfig(
                url = Url(config.getString("pdl.url")),
            )
            val pdlClient = PdlHttpClient(httpClient, pdlClientConfig)
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
