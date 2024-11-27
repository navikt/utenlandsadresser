package no.nav.utenlandsadresser.domain

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Organisasjonsnummer(
    val value: String,
)
