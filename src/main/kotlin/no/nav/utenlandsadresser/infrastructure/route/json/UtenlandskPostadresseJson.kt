package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
data class UtenlandskPostadresseJson(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String?,
    val land: String?,
) {
    companion object {
        fun fromDomain(postadresse: Postadresse): UtenlandskPostadresseJson? = when (postadresse) {
            is Postadresse.Norsk,
            Postadresse.Empty -> null

            is Postadresse.Utenlandsk -> fromDomain(postadresse)
        }

        private fun fromDomain(postadresse: Postadresse.Utenlandsk): UtenlandskPostadresseJson =
            UtenlandskPostadresseJson(
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