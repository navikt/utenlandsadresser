package no.nav.utenlandsadresser.adapter.inbound.web.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet

@Serializable
data class HentUtenlandskIdDevRequestJson(
    val identitetsnummer: String,
)

@Serializable
data class UtenlandskIdentitetDevResponseJson(
    val identitetsnummer: String,
    val utstederland: String,
    val kilde: String,
) {
    companion object {
        fun fromDomain(identitet: UtenlandskIdentitet) =
            UtenlandskIdentitetDevResponseJson(
                identitetsnummer = identitet.identitetsnummer.identitetsnummer,
                utstederland = identitet.utstederland.value,
                kilde = identitet.kilde.kilde,
            )
    }
}
