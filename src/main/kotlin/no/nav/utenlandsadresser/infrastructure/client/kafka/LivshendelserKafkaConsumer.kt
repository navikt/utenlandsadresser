package no.nav.utenlandsadresser.infrastructure.client.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import no.nav.utenlandsadresser.infrastructure.client.LivshendelserConsumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import java.time.Duration

class LivshendelserKafkaConsumer(
    private val kafkaConsumer: KafkaConsumer<String, String>,
    private val logger: Logger,
) : LivshendelserConsumer {
    override fun CoroutineScope.consumeLivshendelser(topic: String) {
        kafkaConsumer.use { kafkaConsumer ->
            try {
                logger.info("Starting Kafka consumer in thread: ${Thread.currentThread().name}")
                kafkaConsumer.subscribe(listOf(topic))
                while (isActive) {
                    val records = kafkaConsumer.poll(Duration.ofSeconds(1))
                    records.forEach {
                        logger.info("Received message: ${it.value()}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in Kafka consumer", e)
                throw e
            }
        }
    }
}
