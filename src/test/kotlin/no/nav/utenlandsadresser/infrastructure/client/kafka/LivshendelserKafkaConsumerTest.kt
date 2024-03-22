package no.nav.utenlandsadresser.infrastructure.client.kafka

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import no.nav.utenlandsadresser.infrastructure.kafka.avro.LivshendelseAvro
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LivshendelserKafkaConsumerTest : WordSpec({
    val partition = TopicPartition("leesah", 0)
    val consumer = MockConsumer<String, LivshendelseAvro>(OffsetResetStrategy.EARLIEST).apply {
        assign(listOf(partition))
        updateBeginningOffsets(mapOf(partition to 0L))
    }

    "livshendelser consumer" should {
        "consume livshendelser" {
            val value = LivshendelseAvro(
                listOf("12345678901"),
                "BOSTEDSADRESSE_V1",
                null,
            )
            consumer.addRecord(ConsumerRecord("leesah", 0, 0, null, value))

            val records = consumer.poll(5.seconds.toJavaDuration())
            records.count() shouldBe 1
        }
    }
})
