package no.nav.utenlandsadresser.setup

import no.nav.person.pdl.leesah.Personhendelse
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.EventConsumers
import no.nav.utenlandsadresser.Repositories
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.kafkConsumerConfig
import no.nav.utenlandsadresser.infrastructure.kafka.KafkaPersonhendelseConsumer
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.MockConsumer
import org.slf4j.LoggerFactory

/**
 * Sette opp alle event consumers som brukes av applikasjonen.
 *
 * @see EventConsumers
 */
context(appEnv: AppEnv, config: UtenlandsadresserConfig)
fun setupEventConsumers(repositories: Repositories): EventConsumers {
    val kafkaConsumer: Consumer<String, Personhendelse> =
        when (appEnv) {
            AppEnv.LOCAL -> {
                MockConsumer("latest")
            }

            AppEnv.DEV_GCP,
            AppEnv.PROD_GCP,
            -> {
                KafkaConsumer(
                    kafkConsumerConfig(config.kafka),
                )
            }
        }

    kafkaConsumer.subscribe(listOf(config.kafka.topic))

    return EventConsumers(
        livshendelserConsumer =
            KafkaPersonhendelseConsumer(
                kafkaConsumer,
                repositories.feedEventCreator,
                LoggerFactory.getLogger(KafkaPersonhendelseConsumer::class.java),
            ),
    )
}
