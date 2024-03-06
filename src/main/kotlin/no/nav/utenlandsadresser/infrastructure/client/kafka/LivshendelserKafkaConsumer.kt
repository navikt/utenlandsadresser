package no.nav.utenlandsadresser.infrastructure.client.kafka

import com.github.avrokotlin.avro4k.Avro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.utenlandsadresser.infrastructure.client.LivshendelserConsumer
import no.nav.utenlandsadresser.infrastructure.client.kafka.avro.LivshendelseAvro
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
                    val consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(5))

                    val livshendelser = consumerRecords.mapNotNull { consumerRecord ->
                        avro.fromRecord(LivshendelseAvro.serializer(), consumerRecord.value())
                    }.mapNotNull(LivshendelseAvro::toDomain)

                    if (livshendelser.isNotEmpty()) {
                        logger.info("Received ${livshendelser.size} livshendelser: $livshendelser")
                    }

                    livshendelser.forEach { livshendelse ->
                        feedEventCreator.createFeedEvent(livshendelse)
                    }

                    kafkaConsumer.commitSync()
                } catch (e: Exception) {
                    val duration = 10.seconds
                    logger.error("Error consuming livshendelser. Waiting $duration seconds before retrying", e)
                    delay(duration)
                }
            }
        }
    }
}
