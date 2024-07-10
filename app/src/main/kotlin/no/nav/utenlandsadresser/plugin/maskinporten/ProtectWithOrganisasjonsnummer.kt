package no.nav.utenlandsadresser.plugin.maskinporten

import arrow.core.getOrElse
import arrow.core.raise.either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

fun Route.protectWithOrganisasjonsnummer(orgnummer: Set<Organisasjonsnummer>) {
    intercept(ApplicationCallPipeline.Call) {
        val organisasjonsnummer = call.extractOrganisasjonsnummer().getOrElse {
            when (it) {
                MaskinportenConsumerError.MissingPrincipal -> call.respond(
                    HttpStatusCode.Unauthorized,
                    "Missing JWT principal"
                )

                MaskinportenConsumerError.MissingConsumerClaim -> call.respond(
                    HttpStatusCode.Unauthorized,
                    "Missing consumer claim in JWT token"
                )

                MaskinportenConsumerError.MissingConsumerIdClaim -> call.respond(
                    HttpStatusCode.Unauthorized,
                    "Missing consumer.ID claim in JWT token"
                )
            }
            return@intercept finish()
        }
        if (organisasjonsnummer !in orgnummer) {
            call.respond(HttpStatusCode.Forbidden, "Consumer is not authorized to access this resource")
            return@intercept finish()
        }
        call.attributes.put(OrganisasjonsnummerKey, organisasjonsnummer.value)
    }
}

fun ApplicationCall.extractOrganisasjonsnummer() = either {
    val principal = principal<JWTPrincipal>()
        ?: raise(MaskinportenConsumerError.MissingPrincipal)

    val consumerClaim = principal.payload.getClaim("consumer")
    if (consumerClaim.isMissing) {
        raise(MaskinportenConsumerError.MissingConsumerClaim)
    }

    // Kommer som en ISO6523-formatert streng, f.eks. "0192:889640782"
    val iso6523OrgNummer = (consumerClaim.asMap()["ID"] as? String)
        ?: raise(MaskinportenConsumerError.MissingConsumerIdClaim)

    val organisasjonsnummer = Organisasjonsnummer(iso6523OrgNummer.split(":").last())
    attributes.put(OrganisasjonsnummerKey, organisasjonsnummer.value)
    organisasjonsnummer
}

val OrganisasjonsnummerKey = AttributeKey<String>("Organisasjonsnummer")

sealed class MaskinportenConsumerError {
    data object MissingPrincipal : MaskinportenConsumerError()
    data object MissingConsumerClaim : MaskinportenConsumerError()
    data object MissingConsumerIdClaim : MaskinportenConsumerError()
}
