package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

@Serializable
data class SporingsloggJson(
    val identitetsnummer: Identitetsnummer,
    val organisasjonsnummer: Organisasjonsnummer,
    val dataTilLogging: JsonElement,
)