package no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
sealed class SporingsloggDto {
    @Serializable
    data class SporingsloggPostadresse(
        val adresselinje1: String?,
        val adresselinje2: String?,
        val adresselinje3: String?,
        val postnummer: String?,
        val poststed: String?,
        val landkode: String,
        val land: String
    ) : SporingsloggDto() {
        companion object {
            fun fromDomain(postadresse: Postadresse.Utenlandsk): SporingsloggPostadresse {
                return SporingsloggPostadresse(
                    postadresse.adresselinje1?.value,
                    postadresse.adresselinje2?.value,
                    postadresse.adresselinje3?.value,
                    postadresse.postnummer?.value,
                    postadresse.poststed?.value,
                    postadresse.landkode.value,
                    postadresse.land.value
                )
            }
        }
    }
}