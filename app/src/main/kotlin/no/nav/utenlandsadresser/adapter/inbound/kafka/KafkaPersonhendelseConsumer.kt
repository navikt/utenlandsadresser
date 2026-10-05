package no.nav.utenlandsadresser.adapter.inbound.kafka

import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.delay
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.utenlandsadresser.adapter.health.HealthCheck
import no.nav.utenlandsadresser.application.port.inbound.HåndterLivshendelse
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Livshendelse
import org.apache.kafka.clients.consumer.Consumer
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaDuration

class KafkaPersonhendelseConsumer(
    private val kafkaConsumer: Consumer<String, Personhendelse>,
    private val håndterLivshendelse: HåndterLivshendelse,
    private val clock: Clock,
) : LivshendelserConsumer,
    Closeable by kafkaConsumer,
    HealthCheck {
    private val logger = LoggerFactory.getLogger(KafkaPersonhendelseConsumer::class.java)

    private var lastPoll: Instant = clock.now()

    override suspend fun consumePersonhendelser() {
        try {
            val consumerRecords = kafkaConsumer.poll(5.seconds.toJavaDuration())

            val livshendelser =
                consumerRecords
                    .mapNotNull {
                        it.value()
                    }.mapNotNull(Personhendelse::toDomain)

            livshendelser.forEach { livshendelse ->
                håndterLivshendelse.håndter(livshendelse)
            }

            kafkaConsumer.commitSync()
            lastPoll = clock.now()
        } catch (e: Exception) {
            val duration = 10.seconds
            logger.error("Error consuming livshendelser. Waiting $duration seconds before retrying", e)
            delay(duration)
        }
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
                gradering = adressebeskyttelse?.gradering?.toDomain() ?: Livshendelse.Adressebeskyttelse.Gradering.UGRADERT,
            )
        }

        null -> {
            null
        }
    }
}

private fun Gradering.toDomain(): Livshendelse.Adressebeskyttelse.Gradering =
    when (this) {
        Gradering.STRENGT_FORTROLIG_UTLAND -> Livshendelse.Adressebeskyttelse.Gradering.STRENGT_FORTROLIG_UTLAND
        Gradering.STRENGT_FORTROLIG -> Livshendelse.Adressebeskyttelse.Gradering.STRENGT_FORTROLIG
        Gradering.FORTROLIG -> Livshendelse.Adressebeskyttelse.Gradering.FORTROLIG
        Gradering.UGRADERT -> Livshendelse.Adressebeskyttelse.Gradering.UGRADERT
    }
