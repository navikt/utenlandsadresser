package no.nav.utenlandsadresser.infrastructure.client.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import no.nav.utenlandsadresser.infrastructure.client.LivshendelserConsumer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.FeedEventCreator
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import java.time.Duration

class LivshendelserKafkaConsumer(
    private val kafkaConsumer: KafkaConsumer<String, LivshendelseAvro>,
    private val feedEventCreator: FeedEventCreator,
    private val logger: Logger,
) : LivshendelserConsumer {
    override suspend fun CoroutineScope.consumeLivshendelser(topic: String) {
        kafkaConsumer.use { kafkaConsumer ->
            kafkaConsumer.subscribe(listOf(topic))
            while (isActive) {
                try {
                    val records = kafkaConsumer.poll(Duration.ofSeconds(5))
                    val livshendelser = records.mapNotNull { consumerRecord ->
                        Livshendelse.from(consumerRecord.value())
                    }
                    livshendelser.forEach { livshendelse ->
                        logger.info("Received livshendelse: $livshendelse")
                        feedEventCreator.createFeedEvent(livshendelse)
                    }
                    kafkaConsumer.commitSync()
                } catch (e: Exception) {
                    logger.error("Error consuming livshendelser", e)
                }
            }
        }
    }
}
