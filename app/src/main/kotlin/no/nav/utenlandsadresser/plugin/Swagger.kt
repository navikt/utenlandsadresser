package no.nav.utenlandsadresser.plugin

import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiDocDsl
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.hide
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Route.configureOpenApi() {
    val source = OpenApiDocSource.Routing()
    val document = OpenApiDoc.build { configureApiInfo() }

    get("/docs/swagger/api.json") {
        val specification = source.read(call.application, document)
        call.respondText(specification.content, specification.contentType)
    }.hide()

    swaggerUI("/docs/swagger") {
        configureApiInfo()
        this.source = source
        remotePath = "api.json"
    }
}

private fun OpenApiDocDsl.configureApiInfo() {
    info = OpenApiInfo("Utenlandsadresser", "latest", "API for å hente utenlandsadresser")
    servers {
        server("https://utenlandsadresser.ekstern.dev.nav.no")
        server("https://utenlandsadresser.nav.no")
        server("http://localhost:8080")
    }
}
