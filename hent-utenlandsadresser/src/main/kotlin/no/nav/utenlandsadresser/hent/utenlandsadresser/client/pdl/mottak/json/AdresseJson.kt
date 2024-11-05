package no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Adressemodell for utenlandsk adresse.
 *
 * @property bygningEtasjeLeilighet Informasjon som navn på bygg, etasje i et bygg, leilighetsnummer o.l. Eksempel: Bldg 14 Apt C Unit 16 Fl 5 Ste 202 Dept 001A Rm 1024
 * @property postkode Postkoden til byen eller stedet. Eksempel: 33461
 * @property bySted Byen eller sted. Eksempel: Lake Worth
 * @property regionDistriktOmraade Region, distrikt og/eller område. Eksempel: FL
 * @property landkode Trebokstavslandkode (ISO-3166-1 alpha-3). Eksempel: USA
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("@type")
sealed class AdresseJson {
    abstract val adressenavnNummer: String?
    abstract val bygningEtasjeLeilighet: String?
    abstract val postboksNummerNavn: String?
    abstract val postkode: String?
    abstract val bySted: String?
    abstract val regionDistriktOmraade: String?
    abstract val landkode: String

    @Serializable
    @SerialName("UTENLANDSK_ADRESSE")
    data class Utenlandsk(
        override val adressenavnNummer: String,
        override val bygningEtasjeLeilighet: String?,
        override val postboksNummerNavn: String?,
        override val postkode: String?,
        override val bySted: String?,
        override val regionDistriktOmraade: String?,
        override val landkode: String,
    ) : AdresseJson()
}
