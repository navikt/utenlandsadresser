package no.nav.utenlandsadresser.infrastructure.route.common.json

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * `abonnementId` sendes som streng. Er strengen ikke en gyldig UUID, feiler deserialiseringen, og Ktor svarer 400.
 */
@Serializable
data class StoppAbonnementJson(
    val abonnementId: Uuid,
)
