package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.infrastructure.kafka.KafkaPersonhendelseConsumer

data class EventConsumers(
    val livshendelserConsumer: KafkaPersonhendelseConsumer,
)