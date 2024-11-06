package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.infrastructure.kafka.KafkaLivshendelserConsumer

data class EventConsumers(
    val livshendelserConsumer: KafkaLivshendelserConsumer,
)