package no.nav.utenlandsadresser.infrastructure.kafka

import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.delay
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.utenlandsadresser.app.LivshendelserConsumer
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresFeedEventCreator
import no.nav.utenlandsadresser.infrastructure.route.HealthCheck
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaDuration

class KafkaPersonhendelseConsumer(
    private val kafkaConsumer: Consumer<String, Personhendelse>,
    private val feedEventCreator: PostgresFeedEventCreator,
    private val clock: Clock,
) : LivshendelserConsumer,
    Closeable by kafkaConsumer,
    HealthCheck {
    private val logger = LoggerFactory.getLogger(KafkaPersonhendelseConsumer::class.java)

    private var lastPoll: Instant = clock.now()

    override suspend fun consumePersonhendelser() {
        var førsteOffsetPerPartisjon: Map<TopicPartition, Long> = emptyMap()
        try {
            val consumerRecords = kafkaConsumer.poll(5.seconds.toJavaDuration())
            førsteOffsetPerPartisjon =
                consumerRecords.partitions().associateWith { consumerRecords.records(it).first().offset() }

            val livshendelser =
                consumerRecords
                    .mapNotNull {
                        it.value()
                    }.mapNotNull(Personhendelse::toDomain)

            livshendelser.forEach { livshendelse ->
                feedEventCreator.createFeedEvent(livshendelse)
            }

            kafkaConsumer.commitSync()
            lastPoll = clock.now()
        } catch (e: Exception) {
            spolTilbake(førsteOffsetPerPartisjon)
            val duration = 10.seconds
            logger.error("Error consuming livshendelser. Waiting $duration before retrying", e)
            delay(duration)
        }
    }

    /**
     * `poll()` flytter posisjonen forbi batchen, også når vi ikke committer. Uten `seek` leser neste `poll()` videre,
     * og neste `commitSync()` committer forbi hendelsene som feilet. Da er de tapt.
     *
     * Partisjoner vi har mistet i en rebalansering, hopper vi over. Den nye eieren starter fra siste commit.
     */
    private fun spolTilbake(førsteOffsetPerPartisjon: Map<TopicPartition, Long>) {
        val tildelt = kafkaConsumer.assignment()
        førsteOffsetPerPartisjon
            .filterKeys { it in tildelt }
            .forEach { (partisjon, offset) -> kafkaConsumer.seek(partisjon, offset) }
    }

    override fun isHealthy(): Boolean {
        val durationSinceLastPoll = (clock.now() - lastPoll).inWholeSeconds
        return durationSinceLastPoll < 60
    }
}

fun Personhendelse.toDomain(): Livshendelse? {
    val personidenter = personidenter.map(CharSequence::toString).map(::Identitetsnummer)
    val opplysningstype = Opplysningstype.entries.firstOrNull { it.name == opplysningstype.toString().trim() }
    return when (opplysningstype) {
        Opplysningstype.BOSTEDSADRESSE_V1 -> {
            Livshendelse.Bostedsadresse(
                personidenter = personidenter,
            )
        }

        Opplysningstype.KONTAKTADRESSE_V1 -> {
            Livshendelse.Kontaktadresse(
                personidenter = personidenter,
            )
        }

        Opplysningstype.ADRESSEBESKYTTELSE_V1 -> {
            Livshendelse.Adressebeskyttelse(
                personidenter = personidenter,
                // Om adressebeskyttelse er null så tyder det på at adressebeskyttelsen er fjernet
                adressebeskyttelse = adressebeskyttelse?.gradering ?: Gradering.UGRADERT,
            )
        }

        null -> {
            null
        }
    }
}
