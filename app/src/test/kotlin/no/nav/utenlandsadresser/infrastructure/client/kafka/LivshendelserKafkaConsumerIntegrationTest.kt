package no.nav.utenlandsadresser.infrastructure.client.kafka

import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.infrastructure.kafka.GraderingAvro
import no.nav.utenlandsadresser.infrastructure.kafka.KafkaLivshendelserConsumer
import no.nav.utenlandsadresser.infrastructure.kafka.avro.AdressebeskyttelseAvro
import no.nav.utenlandsadresser.infrastructure.kafka.avro.LivshendelseAvro
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedRepository
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.util.*

@DoNotParallelize
class LivshendelserKafkaConsumerIntegrationTest :
    WordSpec({
        val database = setupDatabase()
        val feedRepository = PostgresFeedRepository(database)
        val abonnementRepository = PostgresAbonnementRepository(database)
        val feedEventCreator = spyk(PostgresFeedEventCreator(feedRepository, abonnementRepository))

        val topic = "leesah"
        val partition = TopicPartition(topic, 0)
        val consumer =
            MockConsumer<String, GenericRecord>(OffsetResetStrategy.EARLIEST).apply {
                assign(listOf(partition))
            }
        val kafkaLivshendelserConsumer =
            KafkaLivshendelserConsumer(
                consumer,
                feedEventCreator,
                LoggerFactory.getLogger("LivshendelserKafkaConsumer"),
            )

        val organisasjonsnummer = Organisasjonsnummer("123456789")
        val identitetsnummer = Identitetsnummer("12345678901")
        val abonnementId = UUID.randomUUID()
        val opprettetTidspunkt = Clock.System.now()
        val abonnement =
            Abonnement(
                abonnementId,
                organisasjonsnummer,
                identitetsnummer,
                opprettetTidspunkt,
            )

        beforeTest {
            consumer.rebalance(listOf(partition))
            consumer.updateBeginningOffsets(mapOf(partition to 0L))
        }

        "livshendelser consumer" should {
            "consume livshendelser and create feed event" {
                with(abonnementRepository) {
                    createAbonnement(abonnement).isRight() shouldBe true
                }

                val value =
                    LivshendelseAvro(
                        listOf(identitetsnummer.value),
                        "BOSTEDSADRESSE_V1",
                        null,
                    )
                consumer.addRecord(ConsumerRecord(topic, 0, 0, null, value))

                with(kafkaLivshendelserConsumer) {
                    consumeLivshendelser(topic)
                }

                val feedEvent =
                    feedRepository.getFeedEvent(
                        organisasjonsnummer,
                        Løpenummer(1),
                    )

                feedEvent shouldBe
                    FeedEvent.Outgoing(
                        identitetsnummer,
                        abonnementId,
                        Hendelsestype.OppdatertAdresse,
                    )
            }

            "not skip livshendelser when they are of different type" {
                with(abonnementRepository) {
                    createAbonnement(abonnement).isRight() shouldBe true
                }

                val adresseoppdatering =
                    LivshendelseAvro(
                        listOf(identitetsnummer.value),
                        "KONTAKTADRESSE_V1",
                        null,
                    )
                val adressebeskyttelse =
                    LivshendelseAvro(
                        listOf(identitetsnummer.value),
                        "ADRESSEBESKYTTELSE_V1",
                        AdressebeskyttelseAvro(
                            GraderingAvro.STRENGT_FORTROLIG_UTLAND,
                        ),
                    )
                consumer.addRecord(ConsumerRecord(topic, 0, 0, null, adresseoppdatering))
                consumer.addRecord(ConsumerRecord(topic, 0, 1, null, adresseoppdatering))
                consumer.addRecord(ConsumerRecord(topic, 0, 2, null, adresseoppdatering))
                consumer.addRecord(ConsumerRecord(topic, 0, 3, null, adressebeskyttelse))

                with(kafkaLivshendelserConsumer) {
                    consumeLivshendelser(topic)
                }

                coVerify(exactly = 4) { feedEventCreator.createFeedEvent(any()) }

                val feedEvents =
                    (1..3).map {
                        feedRepository.getFeedEvent(
                            organisasjonsnummer,
                            Løpenummer(it),
                        )
                    }

                feedEvents shouldContainInOrder
                    listOf(
                        FeedEvent.Outgoing(
                            identitetsnummer,
                            abonnementId,
                            Hendelsestype.OppdatertAdresse,
                        ),
                        FeedEvent.Outgoing(
                            identitetsnummer,
                            abonnementId,
                            Hendelsestype.Adressebeskyttelse(
                                AdressebeskyttelseGradering.GRADERT,
                            ),
                        ),
                        null,
                    )
            }

            "skip duplicate livshendelser when they are within a short period" {
                with(abonnementRepository) {
                    createAbonnement(abonnement).isRight() shouldBe true
                }

                val value =
                    LivshendelseAvro(
                        listOf(identitetsnummer.value),
                        "BOSTEDSADRESSE_V1",
                        null,
                    )
                consumer.addRecord(ConsumerRecord(topic, 0, 0, null, value))
                consumer.addRecord(ConsumerRecord(topic, 0, 1, null, value))
                consumer.addRecord(ConsumerRecord(topic, 0, 2, null, value))

                with(kafkaLivshendelserConsumer) {
                    consumeLivshendelser(topic)
                }

                coVerify(exactly = 3) { feedEventCreator.createFeedEvent(any()) }

                val feedEvents =
                    (1..3).map {
                        feedRepository.getFeedEvent(
                            organisasjonsnummer,
                            Løpenummer(it),
                        )
                    }

                feedEvents shouldContainInOrder
                    listOf(
                        FeedEvent.Outgoing(
                            identitetsnummer,
                            abonnementId,
                            Hendelsestype.OppdatertAdresse,
                        ),
                        null,
                        null,
                    )
            }
        }
    })
