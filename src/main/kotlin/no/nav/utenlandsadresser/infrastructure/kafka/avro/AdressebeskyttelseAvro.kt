package no.nav.utenlandsadresser.infrastructure.kafka.avro

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.infrastructure.kafka.GraderingAvro

@Serializable
data class AdressebeskyttelseAvro(
    val gradering: GraderingAvro?,
)