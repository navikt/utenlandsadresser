package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
data class FeedResponseJson(
    val identitetsnummer: String,
    val utenlandskPostadresse: UtenlandskPostadresseJson,
) {
    companion object {
        fun fromDomain(identitetsnummer: Identitetsnummer, postadresse: Postadresse): FeedResponseJson = FeedResponseJson(
            identitetsnummer = identitetsnummer.value,
            utenlandskPostadresse = when (postadresse) {
                is Postadresse.Norsk,
                Postadresse.Empty -> UtenlandskPostadresseJson.empty()
                is Postadresse.Utenlandsk -> UtenlandskPostadresseJson.fromDomain(postadresse)
            },
        )
    }
}

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
        fun fromDomain(postadresse: Postadresse.Utenlandsk): UtenlandskPostadresseJson = UtenlandskPostadresseJson(
            adresselinje1 = postadresse.adresselinje1?.value,
            adresselinje2 = postadresse.adresselinje2?.value,
            adresselinje3 = postadresse.adresselinje3?.value,
            postnummer = postadresse.postnummer?.value,
            poststed = postadresse.poststed?.value,
            landkode = postadresse.landkode.value,
            land = postadresse.land.value,
        )

        fun empty(): UtenlandskPostadresseJson = UtenlandskPostadresseJson(
            adresselinje1 = null,
            adresselinje2 = null,
            adresselinje3 = null,
            postnummer = null,
            poststed = null,
            landkode = null,
            land = null,
        )
    }
}