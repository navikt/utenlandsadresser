package no.nav.utenlandsadresser.domain

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Identitetsnummer(
    val value: String,
)
