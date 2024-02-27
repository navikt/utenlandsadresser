package no.nav.utenlandsadresser.infrastructure.client.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import no.nav.utenlandsadresser.infrastructure.client.LivshendelserConsumer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import java.time.Duration

class LivshendelserKafkaConsumer(
    private val kafkaConsumer: KafkaConsumer<GenericRecord, GenericRecord>,
    private val logger: Logger,
) : LivshendelserConsumer {
    override fun CoroutineScope.consumeLivshendelser(topic: String) {
        kafkaConsumer.use { kafkaConsumer ->
            try {
                logger.info("Starting Kafka consumer in thread: ${Thread.currentThread().name}")
                kafkaConsumer.subscribe(listOf(topic))
                while (isActive) {
                    val records = kafkaConsumer.poll(Duration.ofSeconds(5))
                    records.mapNotNull { consumerRecord ->
                        with(logger) {
                            Livshendelse.from(consumerRecord.value())
                                // TODO: Remove
                                .also {
                                    if (it == null) {
                                        info("Received not interesting message of type: ${consumerRecord.value()["opplysningstype"]}")
                                    }
                                }
                        }
                    }.forEach {
                        logger.info("Received very interesting message: $it")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in Kafka consumer", e)
                throw e
            }
        }
    }
}
