package no.nav.utenlandsadresser.infrastructure.client.kafka.avro

import kotlinx.serialization.Serializable

@Serializable
data class AdressebeskyttelseAvro(
    val gradering: String,
)