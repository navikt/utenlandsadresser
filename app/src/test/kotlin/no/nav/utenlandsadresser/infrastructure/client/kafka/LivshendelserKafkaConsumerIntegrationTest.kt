package no.nav.utenlandsadresser.infrastructure.client.kafka

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.kafka.KafkaPersonhendelseConsumer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedRepository
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.toJavaInstant
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
            MockConsumer<String, Personhendelse>("earliest").apply {
                assign(listOf(partition))
                updateBeginningOffsets(mapOf(partition to 0L))
            }

        val kafkaPersonhendelseConsumer =
            KafkaPersonhendelseConsumer(
                consumer,
                feedEventCreator,
                LoggerFactory.getLogger("PersonhendelseKafkaConsumer"),
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

        val defaultPersonhendelseBuilder =
            Personhendelse.newBuilder().apply {
                hendelseId = "0"
                master = "PDL"
                opprettet = Clock.System.now().toJavaInstant()
                endringstype = Endringstype.KORRIGERT
            }

        fun MockConsumer<String, Personhendelse>.addRecord(
            offset: Long,
            personhendelse: Personhendelse,
        ) {
            addRecord(ConsumerRecord(topic, 0, offset, null, personhendelse))
        }

        beforeEach {
            consumer.seekToBeginning(listOf(partition))
        }

        "personhendelse consumer" should {
            "consume personhendelse and create feed event" {
                abonnementRepository.createAbonnement(abonnement).isRight() shouldBe true

                val value =
                    Personhendelse
                        .newBuilder(defaultPersonhendelseBuilder)
                        .apply {
                            personidenter = listOf(identitetsnummer.value)
                            opplysningstype = "BOSTEDSADRESSE_V1"
                            adressebeskyttelse = null
                        }.build()

                consumer.addRecord(0L, value)

                kafkaPersonhendelseConsumer.consumePersonhendelser()

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

            "not skip personhendelse when they are of different type" {
                abonnementRepository.createAbonnement(abonnement).isRight() shouldBe true

                val adresseoppdatering =
                    Personhendelse
                        .newBuilder(defaultPersonhendelseBuilder)
                        .apply {
                            personidenter = listOf(identitetsnummer.value)
                            opplysningstype = "KONTAKTADRESSE_V1"
                            adressebeskyttelse = null
                        }.build()

                val adressebeskyttelse =
                    Personhendelse
                        .newBuilder(defaultPersonhendelseBuilder)
                        .apply {
                            personidenter = listOf(identitetsnummer.value)
                            opplysningstype = "ADRESSEBESKYTTELSE_V1"
                            this.adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG_UTLAND)
                        }.build()
                consumer.addRecord(0L, adresseoppdatering)
                consumer.addRecord(1L, adresseoppdatering)
                consumer.addRecord(2L, adressebeskyttelse)
                consumer.addRecord(3L, adresseoppdatering)

                kafkaPersonhendelseConsumer.consumePersonhendelser()

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

            "skip duplicate personhendelser when they are within a short period" {
                abonnementRepository.createAbonnement(abonnement).isRight() shouldBe true

                val value =
                    Personhendelse
                        .newBuilder(defaultPersonhendelseBuilder)
                        .apply {
                            personidenter = listOf(identitetsnummer.value)
                            opplysningstype = "BOSTEDSADRESSE_V1"
                            adressebeskyttelse = null
                        }.build()
                consumer.addRecord(0L, value)
                consumer.addRecord(1L, value)
                consumer.addRecord(2L, value)

                kafkaPersonhendelseConsumer.consumePersonhendelser()

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
