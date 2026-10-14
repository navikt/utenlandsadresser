package no.nav.utenlandsadresser.infrastructure.kafka

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.utenlandsadresser.FastClock
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.common.TopicPartition
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaInstant

private const val TOPIC = "leesah"

private data class ConsumerFixture(
    val consumer: MockConsumer<String, Personhendelse>,
    val feedEventCreator: PostgresFeedEventCreator,
    val clock: FastClock,
    val partition: TopicPartition,
    val kafkaPersonhendelseConsumer: KafkaPersonhendelseConsumer,
)

private fun newConsumerFixture(): ConsumerFixture {
    val partition = TopicPartition(TOPIC, 0)
    val consumer =
        MockConsumer<String, Personhendelse>("earliest").apply {
            assign(listOf(partition))
            updateBeginningOffsets(mapOf(partition to 0L))
        }
    val feedEventCreator = mockk<PostgresFeedEventCreator>()
    val clock = FastClock()

    return ConsumerFixture(
        consumer = consumer,
        feedEventCreator = feedEventCreator,
        clock = clock,
        partition = partition,
        kafkaPersonhendelseConsumer = KafkaPersonhendelseConsumer(consumer, feedEventCreator, clock),
    )
}

private fun personhendelse(
    hendelseId: String,
    ident: String,
    clock: FastClock,
): Personhendelse =
    Personhendelse
        .newBuilder()
        .apply {
            this.hendelseId = hendelseId
            master = "PDL"
            opprettet = clock.now().toJavaInstant()
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(ident)
            opplysningstype = "BOSTEDSADRESSE_V1"
            adressebeskyttelse = null
        }.build()

private fun MockConsumer<String, Personhendelse>.addPersonhendelse(
    offset: Long,
    personhendelse: Personhendelse,
) {
    addRecord(ConsumerRecord(TOPIC, 0, offset, null, personhendelse))
}

class KafkaPersonhendelseConsumerTest :
    WordSpec({
        // Virtuell tid, så delay(10.seconds) i feilhåndteringen ikke gjør testene trege
        coroutineTestScope = true

        "KafkaPersonhendelseConsumer" should {
            "process the records from a failed poll on the next poll" {
                val fixture = newConsumerFixture()
                val identA = "01010100001"
                val identB = "01010100002"
                val behandlet = mutableListOf<Livshendelse>()
                var antallKall = 0

                coEvery { fixture.feedEventCreator.createFeedEvent(any()) } coAnswers {
                    antallKall++
                    if (antallKall == 1) {
                        throw RuntimeException("Databasen er nede")
                    }
                    behandlet += firstArg<Livshendelse>()
                }

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", identA, fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", identA, fixture.clock))
                fixture.consumer.addPersonhendelse(1L, personhendelse("B", identB, fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()

                behandlet.map { it.personidenter } shouldContainExactly
                    listOf(
                        listOf(Identitetsnummer(identA)),
                        listOf(Identitetsnummer(identB)),
                    )
                fixture.consumer.committed(setOf(fixture.partition))[fixture.partition]?.offset() shouldBe 2L
            }

            "not process records before the failed one again" {
                val fixture = newConsumerFixture()
                val identA = "01010100001"
                val identB = "01010100002"
                val behandlet = mutableListOf<Livshendelse>()
                var harFeilet = false

                coEvery { fixture.feedEventCreator.createFeedEvent(any()) } coAnswers {
                    val livshendelse = firstArg<Livshendelse>()
                    if (livshendelse.personidenter == listOf(Identitetsnummer(identB)) && !harFeilet) {
                        harFeilet = true
                        throw RuntimeException("Databasen er nede")
                    }
                    behandlet += livshendelse
                }

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", identA, fixture.clock))
                fixture.consumer.addPersonhendelse(1L, personhendelse("B", identB, fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", identA, fixture.clock))
                fixture.consumer.addPersonhendelse(1L, personhendelse("B", identB, fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()

                behandlet.map { it.personidenter } shouldContainExactly
                    listOf(
                        listOf(Identitetsnummer(identA)),
                        listOf(Identitetsnummer(identB)),
                    )
                fixture.consumer.committed(setOf(fixture.partition))[fixture.partition]?.offset() shouldBe 2L
            }

            "commit the offset after the records are processed" {
                val fixture = newConsumerFixture()
                coEvery { fixture.feedEventCreator.createFeedEvent(any()) } returns Unit

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", "01010100001", fixture.clock))
                fixture.consumer.addPersonhendelse(1L, personhendelse("B", "01010100002", fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()

                fixture.consumer.committed(setOf(fixture.partition))[fixture.partition]?.offset() shouldBe 2L
            }

            "not commit when processing fails" {
                val fixture = newConsumerFixture()
                coEvery { fixture.feedEventCreator.createFeedEvent(any()) } throws
                    RuntimeException("Databasen er nede")

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", "01010100001", fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()

                fixture.consumer.committed(setOf(fixture.partition))[fixture.partition] shouldBe null
            }

            "be healthy less than 60 seconds after the last successful poll" {
                val fixture = newConsumerFixture()
                coEvery { fixture.feedEventCreator.createFeedEvent(any()) } returns Unit

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", "01010100001", fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()
                fixture.clock.nå += 59.seconds

                fixture.kafkaPersonhendelseConsumer.isHealthy() shouldBe true
            }

            "be unhealthy 60 seconds after the last successful poll" {
                val fixture = newConsumerFixture()
                coEvery { fixture.feedEventCreator.createFeedEvent(any()) } returns Unit

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", "01010100001", fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()
                fixture.clock.nå += 60.seconds

                fixture.kafkaPersonhendelseConsumer.isHealthy() shouldBe false
            }

            "not count a failed poll as healthy" {
                val fixture = newConsumerFixture()
                var shouldFail = false
                coEvery { fixture.feedEventCreator.createFeedEvent(any()) } coAnswers {
                    if (shouldFail) {
                        throw RuntimeException("Databasen er nede")
                    }
                }

                fixture.consumer.addPersonhendelse(0L, personhendelse("A", "01010100001", fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()
                fixture.clock.nå += 30.seconds

                shouldFail = true
                fixture.consumer.addPersonhendelse(1L, personhendelse("B", "01010100002", fixture.clock))
                fixture.kafkaPersonhendelseConsumer.consumePersonhendelser()
                fixture.clock.nå += 30.seconds

                fixture.kafkaPersonhendelseConsumer.isHealthy() shouldBe false
            }
        }
    })
