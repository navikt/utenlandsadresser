package no.nav.utenlandsadresser.plugin

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.github.smiley4.ktorswaggerui.data.SwaggerUiSyntaxHighlight
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.configureSwagger() {
    install(SwaggerUI) {
        swagger {
            syntaxHighlight = SwaggerUiSyntaxHighlight.MONOKAI
        }

        info {
            title = "Utenlandsadresser"
            version = "latest"
            description = "API for å hente utenlandsadresser"
        }

        security {
            securityScheme("Maskinporten") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
            }
            defaultSecuritySchemeNames("Maskinporten")
        }

        pathFilter = { _: HttpMethod, url: List<String> ->
            url.contains("postadresse")
        }
        server {
            url = "https://utenlandsadresser.ekstern.dev.nav.no"
        }
        server {
            url = "https://utenlandsadresser.nav.no"
        }
        server {
            url = "http://localhost:8080"
        }
    }
}
