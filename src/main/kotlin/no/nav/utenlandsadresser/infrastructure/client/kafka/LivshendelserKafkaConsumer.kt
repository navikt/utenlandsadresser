package no.nav.utenlandsadresser.infrastructure.client.kafka

import com.github.avrokotlin.avro4k.Avro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.utenlandsadresser.infrastructure.client.LivshendelserConsumer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.FeedEventCreator
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class LivshendelserKafkaConsumer(
    private val kafkaConsumer: KafkaConsumer<String, GenericRecord>,
    private val feedEventCreator: FeedEventCreator,
    private val logger: Logger,
) : LivshendelserConsumer {
    override suspend fun CoroutineScope.consumeLivshendelser(topic: String) {
        val avro = Avro.default
        kafkaConsumer.use { kafkaConsumer ->
            kafkaConsumer.subscribe(listOf(topic))
            while (isActive) {
                try {
                    val records = kafkaConsumer.poll(Duration.ofSeconds(5))
                    records.forEach { consumerRecord ->
                        logger.info("Received record: ${consumerRecord.value()}")
                    }
                    val livshendelser = records.mapNotNull { consumerRecord ->
                        avro.fromRecord(LivshendelseAvro.serializer(), consumerRecord.value())
                    }.map(Livshendelse::from)
                    livshendelser.forEach { livshendelse ->
                        logger.info("Received livshendelse: $livshendelse")
                        // feedEventCreator.createFeedEvent(livshendelse)
                    }
                    kafkaConsumer.commitSync()
                } catch (e: Exception) {
                    logger.error("Error consuming livshendelser", e)
                    delay(30.seconds)
                }
            }
        }
    }
}
