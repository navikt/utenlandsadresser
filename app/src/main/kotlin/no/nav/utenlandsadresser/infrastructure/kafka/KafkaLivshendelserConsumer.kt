package no.nav.utenlandsadresser.infrastructure.kafka

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.decodeFromGenericData
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import no.nav.utenlandsadresser.app.LivshendelserConsumer
import no.nav.utenlandsadresser.infrastructure.kafka.avro.LivshendelseAvro
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.route.HealthCheck
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.slf4j.Logger
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class KafkaLivshendelserConsumer(
    private val kafkaConsumer: Consumer<String, GenericRecord>,
    private val feedEventCreator: PostgresFeedEventCreator,
    private val logger: Logger,
    private val avro: Avro = Avro,
) : LivshendelserConsumer,
    Closeable by kafkaConsumer,
    HealthCheck {
    private var lastPoll: Instant = Clock.System.now()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun consumeLivshendelser() {
        try {
            val consumerRecords = kafkaConsumer.poll(5.seconds.toJavaDuration())

            val livshendelser =
                consumerRecords
                    .mapNotNull { consumerRecord ->
                        avro.decodeFromGenericData<LivshendelseAvro>(consumerRecord.value())
                    }.mapNotNull(LivshendelseAvro::toDomain)

            livshendelser.forEach { livshendelse ->
                feedEventCreator.createFeedEvent(livshendelse)
            }

            kafkaConsumer.commitSync()
            lastPoll = Clock.System.now()
        } catch (e: Exception) {
            val duration = 10.seconds
            logger.error("Error consuming livshendelser. Waiting $duration seconds before retrying", e)
            delay(duration)
        }
    }

    override fun isHealthy(): Boolean {
        val durationSinceLastPoll = (Clock.System.now() - lastPoll).inWholeSeconds
        return durationSinceLastPoll < 60
    }
}
