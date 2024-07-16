package no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.*

@Serializable
data class PostadresseResponseJson(
    val navn: String,
    val adresse: AdresseResponseJson,
)

@Serializable
data class AdresseResponseJson(
    val adresseKilde: String,
    val type: Adressetype,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
    val land: String,
) {
    fun toDomain(): Postadresse =
        when (type) {
            Adressetype.NORSKPOSTADRESSE ->
                Postadresse.Norsk(
                    adresselinje1 = adresselinje1?.let { Adresselinje(it) },
                    adresselinje2 = adresselinje2?.let { Adresselinje(it) },
                    adresselinje3 = adresselinje3?.let { Adresselinje(it) },
                    postnummer = postnummer?.let { Postnummer(it) },
                    poststed = poststed?.let { Poststed(it) },
                    landkode = Landkode(landkode),
                    land = Land(land),
                )

            Adressetype.UTENLANDSKPOSTADRESSE ->
                Postadresse.Utenlandsk(
                    adresselinje1 = adresselinje1?.let { Adresselinje(it) },
                    adresselinje2 = adresselinje2?.let { Adresselinje(it) },
                    adresselinje3 = adresselinje3?.let { Adresselinje(it) },
                    postnummer = postnummer?.let { Postnummer(it) },
                    poststed = poststed?.let { Poststed(it) },
                    landkode = Landkode(landkode),
                    land = Land(land),
                )
        }
}

@Serializable
enum class Adressetype {
    NORSKPOSTADRESSE,
    UTENLANDSKPOSTADRESSE,
}
