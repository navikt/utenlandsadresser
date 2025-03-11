package no.nav.utenlandsadresser.plugin
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.config.ExampleEncoder
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.configureOpenApi() {
    install(OpenApi) {
        info {
            title = "Utenlandsadresser"
            version = "latest"
            description = "API for å hente utenlandsadresser"
        }

        examples {
            exampleEncoder = ExampleEncoder.kotlinx()
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
