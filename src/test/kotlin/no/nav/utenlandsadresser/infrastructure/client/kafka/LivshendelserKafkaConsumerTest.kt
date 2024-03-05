package no.nav.utenlandsadresser.infrastructure.client.kafka

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.WordSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class LivshendelserKafkaConsumerTest : WordSpec({
    val kafka = install(ContainerExtension(KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1")))) {
        withEmbeddedZookeeper()
        withCreateContainerCmdModifier { it.withPlatform("linux/amd64") }
        withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
    }

    val producer = KafkaProducer(
        mapOf("bootstrap.servers" to kafka.bootstrapServers),
        StringSerializer(),
        Avro4kLivshendelseSerializer()
    )
    val consumer = KafkaConsumer(
        mapOf(
            "bootstrap.servers" to kafka.bootstrapServers,
            "group.id" to "test",
            "auto.offset.reset" to "earliest",
        ), StringDeserializer(), Avro4kLivshendelseDeserializer()
    )

    "livshendelser consumer" should {
        "consume livshendelser".config(enabled = false) {
            val value = LivshendelseAvro(
                listOf("12345678901"),
                "BOSTEDSADRESSE_V1",
                null,
            )
            producer.send(ProducerRecord("leesah", null, value)).get()
            producer.close()


            consumer.subscribe(listOf("leesah"))

            val records = consumer.poll(java.time.Duration.ofSeconds(5))
            records.count() shouldBe 1
        }
    }

})
