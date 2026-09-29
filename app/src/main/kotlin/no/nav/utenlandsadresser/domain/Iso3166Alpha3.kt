package no.nav.utenlandsadresser.domain

import java.util.Locale

@JvmInline
value class Iso3166Alpha3(
    val value: String,
) {
    init {
        require(value in codes) { "Landkoden må være en gyldig ISO 3166-1 alpha-3-kode" }
    }

    companion object {
        private val codes by lazy {
            Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA3).toSet()
        }

        fun from(value: String): Iso3166Alpha3? = value.takeIf(codes::contains)?.let(::Iso3166Alpha3)
    }
}
