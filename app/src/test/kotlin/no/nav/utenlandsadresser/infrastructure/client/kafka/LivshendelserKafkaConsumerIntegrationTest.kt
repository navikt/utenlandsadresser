package no.nav.utenlandsadresser.infrastructure.client.kafka

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
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
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Isolate
class LivshendelserKafkaConsumerIntegrationTest :
    WordSpec({
        val database = setupDatabase()
        val feedRepository = PostgresFeedRepository(database)
        val abonnementRepository = PostgresAbonnementRepository(database)
        val feedEventCreator = PostgresFeedEventCreator(feedRepository, abonnementRepository, database)

        val topic = "leesah"
        val partition = TopicPartition(topic, 0)
        val consumer =
            MockConsumer<String, GenericRecord>("earliest").apply {
                assign(listOf(partition))
                updateBeginningOffsets(mapOf(partition to 0L))
            }

        val kafkaLivshendelserConsumer =
            KafkaLivshendelserConsumer(
                consumer,
                feedEventCreator,
                LoggerFactory.getLogger("LivshendelserKafkaConsumer"),
            )

        val organisasjonsnummer = Organisasjonsnummer("123456789")
        val identitetsnummer = Identitetsnummer("12345678901")
        val abonnementId = Uuid.random()
        val opprettetTidspunkt = Clock.System.now()
        val abonnement =
            Abonnement(
                abonnementId,
                organisasjonsnummer,
                identitetsnummer,
                opprettetTidspunkt,
            )

        fun MockConsumer<String, GenericRecord>.addRecord(
            offset: Long,
            livshendelseAvro: LivshendelseAvro,
        ) {
            addRecord(ConsumerRecord(topic, 0, offset, null, livshendelseAvro))
        }

        beforeEach {
            consumer.seekToBeginning(listOf(partition))
        }

        "livshendelser consumer" should {
            "consume livshendelser and create feed event" {
                abonnementRepository.createAbonnement(abonnement).isRight() shouldBe true

                val value =
                    LivshendelseAvro(
                        listOf(identitetsnummer.value),
                        "BOSTEDSADRESSE_V1",
                        null,
                    )
                consumer.addRecord(0L, value)

                kafkaLivshendelserConsumer.consumeLivshendelser()

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
                abonnementRepository.createAbonnement(abonnement).isRight() shouldBe true

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
                consumer.addRecord(0L, adresseoppdatering)
                consumer.addRecord(1L, adresseoppdatering)
                consumer.addRecord(2L, adressebeskyttelse)
                consumer.addRecord(3L, adresseoppdatering)

                kafkaLivshendelserConsumer.consumeLivshendelser()

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
                abonnementRepository.createAbonnement(abonnement).isRight() shouldBe true

                val value =
                    LivshendelseAvro(
                        listOf(identitetsnummer.value),
                        "BOSTEDSADRESSE_V1",
                        null,
                    )
                consumer.addRecord(0L, value)
                consumer.addRecord(1L, value)
                consumer.addRecord(2L, value)

                kafkaLivshendelserConsumer.consumeLivshendelser()

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
