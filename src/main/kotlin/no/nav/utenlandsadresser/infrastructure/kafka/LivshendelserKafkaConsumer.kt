package no.nav.utenlandsadresser.infrastructure.kafka

import com.github.avrokotlin.avro4k.Avro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import no.nav.utenlandsadresser.infrastructure.kafka.avro.LivshendelseAvro
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.FeedEventCreator
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.slf4j.Logger
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class LivshendelserKafkaConsumer(
    private val kafkaConsumer: Consumer<String, GenericRecord>,
    private val feedEventCreator: FeedEventCreator,
    private val logger: Logger,
    private val avro: Avro = Avro.default,
) : LivshendelserConsumer {
    override suspend fun CoroutineScope.consumeLivshendelser(topic: String) {
        kafkaConsumer.use { kafkaConsumer ->
            try {
                val consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(5))

                val livshendelser = consumerRecords.mapNotNull { consumerRecord ->
                    avro.fromRecord(LivshendelseAvro.serializer(), consumerRecord.value())
                }.mapNotNull(LivshendelseAvro::toDomain)

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
