package no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json

import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("@type")
sealed class EndringsmeldingJson {
    abstract val kilde: String

    @Serializable
    @SerialName("UTENLANDSK_KONTAKTADRESSE")
    data class Kontaktadresse(
        override val kilde: String,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val coAdressenavn: String?,
        val adresse: UtenlandskAdresseJson,
    ) : EndringsmeldingJson()
}
