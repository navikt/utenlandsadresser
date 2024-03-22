package no.nav.utenlandsadresser.infrastructure.route.json

import io.github.smiley4.ktorswaggerui.dsl.Example
import kotlinx.serialization.Serializable

@Serializable
data class StartAbonnementRequestJson(
    @Example("12345678901")
    val identitetsnummer: String,
)