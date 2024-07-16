package no.nav.utenlandsadresser.plugin.maskinporten

import arrow.core.raise.either
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.util.AttributeKey
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

val OrganisasjonsnummerKey = AttributeKey<String>("Organisasjonsnummer")

fun ApplicationCall.extractOrganisasjonsnummer(jwtCredential: JWTCredential) =
    either {
        val consumerClaim = jwtCredential.payload.getClaim("consumer")
        if (consumerClaim.isMissing) {
            raise(MaskinportenConsumerError.MissingConsumerClaim)
        }

        // Kommer som en ISO6523-formatert streng, f.eks. "0192:889640782"
        val iso6523OrgNummer =
            (consumerClaim.asMap()["ID"] as? String)
                ?: raise(MaskinportenConsumerError.MissingConsumerIdClaim)

        val organisasjonsnummer = Organisasjonsnummer(iso6523OrgNummer.split(":").last())
        attributes.put(OrganisasjonsnummerKey, organisasjonsnummer.value)
        organisasjonsnummer
    }

fun validateOrganisasjonsnummer(consumers: List<String>): ApplicationCall.(JWTCredential) -> Boolean =
    { credential ->
        val organisasjonsnummer = extractOrganisasjonsnummer(credential)

        organisasjonsnummer.fold(
            { false },
            { consumers.contains(it.value) },
        )
    }

sealed class MaskinportenConsumerError {
    data object MissingConsumerClaim : MaskinportenConsumerError()

    data object MissingConsumerIdClaim : MaskinportenConsumerError()
}
