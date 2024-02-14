package no.nav.utenlandsadresser.domain

sealed class Postadresse {
    data class Utenlandsk(
        val adresselinje1: Adresselinje?,
        val adresselinje2: Adresselinje?,
        val adresselinje3: Adresselinje?,
        val postnummer: Postnummer?,
        val poststed: Poststed?,
        val landkode: Landkode,
        val land: Land
    ) : Postadresse()

    data class Norsk(
        val adresselinje1: Adresselinje?,
        val adresselinje2: Adresselinje?,
        val adresselinje3: Adresselinje?,
        val postnummer: Postnummer?,
        val poststed: Poststed?,
        val landkode: Landkode,
        val land: Land
    ) : Postadresse()

    data object Empty : Postadresse()
}
