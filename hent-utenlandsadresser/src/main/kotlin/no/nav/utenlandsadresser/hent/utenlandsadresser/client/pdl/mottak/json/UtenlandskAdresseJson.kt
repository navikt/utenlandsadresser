package no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Adressemodell for utenlandsk adresse.
 *
 * @property bygningEtasjeLeilighet Informasjon som navn på bygg, etasje i et bygg, leilighetsnummer o.l. Eksempel: Bldg 14 Apt C Unit 16 Fl 5 Ste 202 Dept 001A Rm 1024
 * @property postkode Postkoden til byen eller stedet. Eksempel: 33461
 * @property bySted Byen eller sted. Eksempel: Lake Worth
 * @property regionDistriktOmraade Region, distrikt og/eller område. Eksempel: FL
 * @property landkode Trebokstavslandkode (ISO-3166-1 alpha-3). Eksempel: USA
 * @property type Navnet på adressetypen i upper-case. Eksempel: UTENLANDSK_ADRESSE
 */
@Serializable
sealed class UtenlandskAdresseJson {
    abstract val bygningEtasjeLeilighet: String?
    abstract val postkode: String?
    abstract val bySted: String?
    abstract val regionDistriktOmraade: String?
    abstract val landkode: String

    @SerialName("@type")
    abstract val type: Type

    enum class Type {
        UTENLANDSK_ADRESSE,
        POSTBOKSADRESSE,
        VEGADRESSE,
    }

    /**
     * Utenlandsk adresse med addressenavn og nummer.
     *
     * @property adressenavnNummer Gatenavn og husnummer o.l. Eksempel: 40 NE. Lyme Ave
     */
    @Serializable
    data class MedAdressenavnNummer(
        val adressenavnNummer: String,
        override val bygningEtasjeLeilighet: String?,
        override val postkode: String?,
        override val bySted: String?,
        override val regionDistriktOmraade: String?,
        override val landkode: String,
    ) : UtenlandskAdresseJson() {
        @SerialName("@type")
        override val type: Type = Type.UTENLANDSK_ADRESSE
    }

    /**
     * Utenlandsk adresse med postboksnummer og navn.
     *
     * @property postboksNummerNavn Postboksnummer og navn på postbokseier. Eksempel: Po.box 15, Fornavn Etternavn
     */
    @Serializable
    data class MedPostboksNummerNavn(
        val postboksNummerNavn: String,
        override val bygningEtasjeLeilighet: String?,
        override val postkode: String?,
        override val bySted: String?,
        override val regionDistriktOmraade: String?,
        override val landkode: String,
    ) : UtenlandskAdresseJson() {
        @SerialName("@type")
        override val type: Type = Type.UTENLANDSK_ADRESSE
    }
}
