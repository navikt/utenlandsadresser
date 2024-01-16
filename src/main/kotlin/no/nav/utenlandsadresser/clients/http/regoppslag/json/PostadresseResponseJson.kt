package no.nav.utenlandsadresser.clients.http.regoppslag.json

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
    fun toDomain(): Postadresse {
        return when (type) {
            Adressetype.NorskPostadresse -> Postadresse.Norsk(
                adresselinje1 = adresselinje1?.let { Adresselinje(it) },
                adresselinje2 = adresselinje2?.let { Adresselinje(it) },
                adresselinje3 = adresselinje3?.let { Adresselinje(it) },
                postnummer = postnummer?.let { Postnummer(it) },
                poststed = poststed?.let { Poststed(it) },
                landkode = Landkode(landkode),
                land = Land(land),
            )

            Adressetype.UtenlandskPostadresse -> Postadresse.Utenlandsk(
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
}

@Serializable
enum class Adressetype {
    NorskPostadresse,
    UtenlandskPostadresse,
}