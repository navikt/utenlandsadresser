package no.nav.utenlandsadresser.domain

sealed class Postadresse {
    abstract val adresselinje1: Adresselinje?
    abstract val adresselinje2: Adresselinje?
    abstract val adresselinje3: Adresselinje?
    abstract val postnummer: Postnummer?
    abstract val poststed: Poststed?
    abstract val landkode: Landkode
    abstract val land: Land

    data class Utenlandsk(
        override val adresselinje1: Adresselinje?,
        override val adresselinje2: Adresselinje?,
        override val adresselinje3: Adresselinje?,
        override val postnummer: Postnummer?,
        override val poststed: Poststed?,
        override val landkode: Landkode,
        override val land: Land
    ) : Postadresse()

    data class Norsk(
        override val adresselinje1: Adresselinje?,
        override val adresselinje2: Adresselinje?,
        override val adresselinje3: Adresselinje?,
        override val postnummer: Postnummer?,
        override val poststed: Poststed?,
        override val landkode: Landkode,
        override val land: Land
    ) : Postadresse()
}
