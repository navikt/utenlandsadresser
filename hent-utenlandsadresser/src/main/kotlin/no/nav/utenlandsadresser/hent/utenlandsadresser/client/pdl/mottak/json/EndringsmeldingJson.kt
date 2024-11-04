package no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class EndringsmeldingJson {
    abstract val kilde: String

    @SerialName("@type")
    val type: String = "KONTAKTADRESSE"

    @Serializable
    data class Kontaktadresse(
        override val kilde: String,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val coAdressenavn: String?,
        val adresse: UtenlandskAdresseJson,
    ) : EndringsmeldingJson()
}
