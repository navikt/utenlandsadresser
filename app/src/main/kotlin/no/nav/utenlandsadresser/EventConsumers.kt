package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.adapter.inbound.kafka.KafkaPersonhendelseConsumer

data class EventConsumers(
    val livshendelserConsumer: KafkaPersonhendelseConsumer,
)