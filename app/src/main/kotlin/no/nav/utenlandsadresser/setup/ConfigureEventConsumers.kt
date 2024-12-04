package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.EventConsumers
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.kafkConsumerConfig
import no.nav.utenlandsadresser.infrastructure.kafka.KafkaLivshendelserConsumer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory

/**
 * Sette opp alle event consumers som brukes av applikasjonen.
 *
 * @see EventConsumers
 */
fun configureEventConsumers(
    appEnv: AppEnv,
    config: UtenlandsadresserConfig,
    repositories: Repositories,
): EventConsumers {
    val kafkaConsumer: Consumer<String, GenericRecord> =
        when (appEnv) {
            AppEnv.LOCAL -> MockConsumer(OffsetResetStrategy.LATEST)
            AppEnv.DEV_GCP,
            AppEnv.PROD_GCP,
            ->
                KafkaConsumer(
                    kafkConsumerConfig(config.kafka),
                )
        }

    kafkaConsumer.subscribe(listOf(config.kafka.topic))

    return EventConsumers(
        livshendelserConsumer =
            KafkaLivshendelserConsumer(
                kafkaConsumer,
                repositories.feedEventCreator,
                LoggerFactory.getLogger(KafkaLivshendelserConsumer::class.java),
            ),
    )
}
