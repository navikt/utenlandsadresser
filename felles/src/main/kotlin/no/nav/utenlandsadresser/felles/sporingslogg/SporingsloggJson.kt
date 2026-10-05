package no.nav.utenlandsadresser.felles.sporingslogg

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SporingsloggJson(
    val identitetsnummer: String,
    val organisasjonsnummer: String,
    val dataTilLogging: JsonElement,
)
