package no.nav.utenlandsadresser.plugins.security

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

fun Application.configureSecurity(
    devApiCredentials: DevApiCredentials?,
) {
    authentication {
        val validate: suspend ApplicationCall.(UserPasswordCredential) -> Principal? = { credentials ->
            devApiCredentials?.let {
                if (credentials.name == it.name && credentials.password == it.password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }

        basic(name = "basic-dev-auth") {
            realm = "Ktor Server"
            validate(validate)
        }

        form(name = "form-dev-auth") {
            userParamName = "user"
            passwordParamName = "password"
            validate(validate)
            challenge {
                call.respond(UnauthorizedResponse())
            }
        }
    }
}



