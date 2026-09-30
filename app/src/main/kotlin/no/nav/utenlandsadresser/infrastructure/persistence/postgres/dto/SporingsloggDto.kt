package no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
sealed class SporingsloggDto {
    abstract fun encodeToJsonElement(json: Json = Json): JsonElement

    @Serializable
    data class SporingsloggPostadresse(
        val adresselinje1: String?,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?,
        val poststed: String?,
        val landkode: String,
        val land: String,
    ) : SporingsloggDto() {
        companion object {
            fun fromDomain(postadresse: Postadresse.Utenlandsk): SporingsloggPostadresse =
                SporingsloggPostadresse(
                    postadresse.adresselinje1?.value,
                    postadresse.adresselinje2?.value,
                    postadresse.adresselinje3?.value,
                    postadresse.postnummer?.value,
                    postadresse.poststed?.value,
                    postadresse.landkode.value,
                    postadresse.land.value,
                )
        }

        override fun encodeToJsonElement(json: Json): JsonElement = json.encodeToJsonElement(serializer(), this)
    }

    @Serializable
    data class SporingsloggUtenlandskId(
        val utenlandskId: List<UtenlandskIdentitet>,
    ) : SporingsloggDto() {
        @Serializable
        data class UtenlandskIdentitet(
            val identitetsnummer: String,
            val utstederland: String,
            val kilde: String,
        )

        companion object {
            fun fromDomain(utenlandskeIdentiteter: List<no.nav.utenlandsadresser.domain.UtenlandskIdentitet>): SporingsloggUtenlandskId =
                SporingsloggUtenlandskId(
                    utenlandskeIdentiteter.map {
                        UtenlandskIdentitet(
                            identitetsnummer = it.identitetsnummer.identitetsnummer,
                            utstederland = it.utstederland.value,
                            kilde = it.kilde.kilde,
                        )
                    },
                )
        }

        override fun encodeToJsonElement(json: Json): JsonElement = json.encodeToJsonElement(serializer(), this)
    }
}
