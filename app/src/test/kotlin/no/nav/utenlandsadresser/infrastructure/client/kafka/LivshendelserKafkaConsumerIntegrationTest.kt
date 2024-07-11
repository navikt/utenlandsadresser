package no.nav.utenlandsadresser.infrastructure.client.kafka

import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.spyk
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.infrastructure.kafka.LivshendelserKafkaConsumer
import no.nav.utenlandsadresser.infrastructure.kafka.avro.LivshendelseAvro
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.AbonnementPostgresRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.FeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.FeedPostgresRepository
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.util.*

@DoNotParallelize
class LivshendelserKafkaConsumerIntegrationTest : WordSpec({
    val database = setupDatabase()
    val feedRepository = FeedPostgresRepository(database)
    val abonnementRepository = AbonnementPostgresRepository(database)
    val feedEventCreator = spyk(FeedEventCreator(feedRepository, abonnementRepository))

    val partition = TopicPartition("leesah", 0)
    val consumer = MockConsumer<String, GenericRecord>(OffsetResetStrategy.EARLIEST).apply {
        assign(listOf(partition))
    }
    val livshendelserKafkaConsumer =
        LivshendelserKafkaConsumer(consumer, feedEventCreator, LoggerFactory.getLogger("LivshendelserKafkaConsumer"))

    val organisasjonsnummer = Organisasjonsnummer("123456789")
    val identitetsnummer = Identitetsnummer("12345678901")
    val abonnementId = UUID.randomUUID()
    val opprettetTidspunkt = Clock.System.now()
    val abonnement = Abonnement(
        abonnementId,
        organisasjonsnummer,
        identitetsnummer,
        opprettetTidspunkt
    )

    beforeTest {
        consumer.updateBeginningOffsets(mapOf(partition to 0L))
    }

    "livshendelser consumer" should {
        "consume livshendelser and create feed event" {
            with(abonnementRepository) {
                createAbonnement(abonnement).isRight() shouldBe true
            }

            val value = LivshendelseAvro(
                listOf(identitetsnummer.value),
                "BOSTEDSADRESSE_V1",
                null,
            )
            consumer.addRecord(ConsumerRecord("leesah", 0, 0, null, value))

            with(livshendelserKafkaConsumer) {
                consumeLivshendelser("leesah")
            }

            val feedEvent = feedRepository.getFeedEvent(
                organisasjonsnummer,
                Løpenummer(1)
            )

            feedEvent shouldBe FeedEvent.Outgoing(
                identitetsnummer,
                abonnementId,
                Hendelsestype.OppdatertAdresse
            )
        }

        "consume skip duplicate livshendelser when they are within a short period" {
            with(abonnementRepository) {
                createAbonnement(abonnement).isRight() shouldBe true
            }

            val value = LivshendelseAvro(
                listOf(identitetsnummer.value),
                "BOSTEDSADRESSE_V1",
                null,
            )
            consumer.addRecord(ConsumerRecord("leesah", 0, 0, null, value))
            consumer.addRecord(ConsumerRecord("leesah", 0, 1, null, value))
            consumer.addRecord(ConsumerRecord("leesah", 0, 2, null, value))

            with(livshendelserKafkaConsumer) {
                consumeLivshendelser("leesah")
            }

            coVerify(exactly = 3) { feedEventCreator.createFeedEvent(any()) }

            feedRepository.getFeedEvent(
                organisasjonsnummer,
                Løpenummer(1)
            ) shouldBe FeedEvent.Outgoing(
                identitetsnummer,
                abonnementId,
                Hendelsestype.OppdatertAdresse
            )
            feedRepository.getFeedEvent(
                organisasjonsnummer,
                Løpenummer(2)
            ) shouldBe null
            feedRepository.getFeedEvent(
                organisasjonsnummer,
                Løpenummer(3)
            ) shouldBe null
        }
    }
})
