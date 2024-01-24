package no.nav.utenlandsadresser.routes.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
data class PostadresseResponseJson(
    val type: String,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
    val land: String,
) {
    companion object {
        fun fromDomain(postadresse: Postadresse): PostadresseResponseJson = when (postadresse) {
            is Postadresse.Utenlandsk -> PostadresseResponseJson(
                type = "UTENLANDSK",
                adresselinje1 = postadresse.adresselinje1?.value,
                adresselinje2 = postadresse.adresselinje2?.value,
                adresselinje3 = postadresse.adresselinje3?.value,
                postnummer = postadresse.postnummer?.value,
                poststed = postadresse.poststed?.value,
                landkode = postadresse.landkode.value,
                land = postadresse.land.value,
            )

            is Postadresse.Norsk -> PostadresseResponseJson(
                type = "NORSK",
                adresselinje1 = postadresse.adresselinje1?.value,
                adresselinje2 = postadresse.adresselinje2?.value,
                adresselinje3 = postadresse.adresselinje3?.value,
                postnummer = postadresse.postnummer?.value,
                poststed = postadresse.poststed?.value,
                landkode = postadresse.landkode.value,
                land = postadresse.land.value,
            )
        }
    }
}