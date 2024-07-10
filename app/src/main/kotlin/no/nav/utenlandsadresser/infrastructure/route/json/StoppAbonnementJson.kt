package no.nav.utenlandsadresser.infrastructure.route.json

import io.github.smiley4.ktorswaggerui.dsl.Example
import kotlinx.serialization.Serializable

@Serializable
data class StoppAbonnementJson(
    @Example("f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f")
    val abonnementId: String,
)
