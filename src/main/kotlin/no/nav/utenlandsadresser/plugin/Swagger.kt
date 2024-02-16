package no.nav.utenlandsadresser.plugin

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.github.smiley4.ktorswaggerui.data.SwaggerUiSyntaxHighlight
import io.ktor.http.*
import io.ktor.server.application.*

fun Application.configureSwagger() {
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "/docs/swagger"
            syntaxHighlight = SwaggerUiSyntaxHighlight.MONOKAI
        }

        info {
            title = "Utenlandsadresser"
            version = "latest"
            description = "API for å hente utenlandsadresser"
        }


        securityScheme("Maskinporten") {
            type = AuthType.HTTP
            scheme = AuthScheme.BEARER
        }

        defaultSecuritySchemeName = "Maskinporten"
        pathFilter = { _: HttpMethod, url: List<String> ->
            url.contains("postadresse")
        }
        server {
            url = "https://utenlandsadresser-gw.ekstern.dev.nav.no"
        }
        server {
            url = "https://utenlandsadresser-gw.ekstern.nav.no"
        }
        server {
            url = "http://localhost:8080"
        }

    }
}