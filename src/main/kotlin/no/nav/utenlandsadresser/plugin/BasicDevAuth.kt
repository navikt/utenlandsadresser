package no.nav.utenlandsadresser.plugin

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import no.nav.utenlandsadresser.plugin.config.BasicAuthConfig

fun Application.configureBasicAuthDev(
    basicAuthConfig: BasicAuthConfig?
) {
    configureBasicAuth("dev") { credentials ->
        basicAuthConfig?.let {
            if (credentials.name == it.username && credentials.password == it.password.value) {
                UserIdPrincipal(credentials.name)
            } else {
                null
            }
        }
    }
}

private fun Application.configureBasicAuth(
    name: String,
    validate: suspend ApplicationCall.(UserPasswordCredential) -> Principal?,
) {
    authentication {
        basic(name = "basic-$name-auth") {
            realm = "Ktor Server"
            validate(validate)
        }

        form(name = "form-$name-auth") {
            userParamName = "user"
            passwordParamName = "password"
            validate(validate)
            challenge {
                call.respond(UnauthorizedResponse())
            }
        }
    }
}


