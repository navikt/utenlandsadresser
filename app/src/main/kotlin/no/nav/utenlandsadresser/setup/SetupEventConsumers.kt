package no.nav.utenlandsadresser.setup

import no.nav.person.pdl.leesah.Personhendelse
import no.nav.utenlandsadresser.EventConsumers
import no.nav.utenlandsadresser.Services
import no.nav.utenlandsadresser.adapter.inbound.kafka.KafkaPersonhendelseConsumer
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.kafkConsumerConfig
import no.nav.utenlandsadresser.felles.AppEnv
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.MockConsumer
import kotlin.time.Clock

/**
 * Sette opp alle event consumers som brukes av applikasjonen.
 *
 * @see EventConsumers
 */
context(appEnv: AppEnv, config: UtenlandsadresserConfig, clock: Clock)
fun setupEventConsumers(services: Services): EventConsumers {
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
                services.håndterLivshendelse,
                clock,
            ),
    )
}
