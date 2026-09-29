package no.nav.utenlandsadresser.domain

data class UtenlandskIdentitet(
    val identitetsnummer: UtenlandskIdentitetsnummer,
    val utstederland: Iso3166Alpha3,
    val kilde: UtenlandskIdentitetKilde,
)
