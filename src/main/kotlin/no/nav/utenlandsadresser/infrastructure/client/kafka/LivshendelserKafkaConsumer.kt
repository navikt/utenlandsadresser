package no.nav.utenlandsadresser.infrastructure.client.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import no.nav.utenlandsadresser.infrastructure.client.LivshendelserConsumer
import no.nav.utenlandsadresser.infrastructure.client.kafka.Livshendelse.Companion.toLivshendelse
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import java.time.Duration

class LivshendelserKafkaConsumer(
    private val kafkaConsumer: KafkaConsumer<GenericRecord, GenericRecord>,
    private val logger: Logger,
) : LivshendelserConsumer {
    override fun CoroutineScope.consumeLivshendelser(topic: String) {
        kafkaConsumer.use { kafkaConsumer ->
            try {
                logger.info("Starting Kafka consumer in thread: ${Thread.currentThread().name}")
                kafkaConsumer.subscribe(listOf(topic))
                while (isActive) {
                    val records = kafkaConsumer.poll(Duration.ofSeconds(1))
                    records.mapNotNull { consumerRecord ->
                        consumerRecord.value().toLivshendelse()
                            // TODO: Remove
                            .also {
                                if (it == null) {
                                    logger.info("Received not interesting message of type: ${consumerRecord.value()["opplysningstype"]}")
                                }
                            }
                    }.forEach {
                        logger.info("Received message: $it")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in Kafka consumer", e)
                throw e
            }
        }
    }
}

sealed class Livshendelse {
    abstract val personidenter: List<String>
    abstract val opplysningsType: Opplysningstype

    companion object {
        fun GenericRecord.toLivshendelse(): Livshendelse? {
            val personidenter = (this["personidenter"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            return when (Opplysningstype.entries.firstOrNull { it.name == this["opplysningstype"] }) {
                Opplysningstype.BOSTEDSADRESSE_V1 -> Bostedsadresse(
                    personidenter = personidenter,
                    utenlandskAdresse = this["utenlandskAdresse"],
                )

                Opplysningstype.KONTAKTADRESSE_V1 -> Kontaktadresse(
                    personidenter = personidenter,
                    utenlandskAdresse = this["utenlandskAdresse"],
                )

                Opplysningstype.ADRESSEBESKYTTELSE_V1 -> Adressebeskyttelse(
                    personidenter = personidenter,
                    adressebeskyttelse = Gradering.valueOf(this["adressebeskyttelse"].toString()),
                )

                else -> null
            }
        }
    }

    data class Bostedsadresse(
        override val personidenter: List<String>,
        val utenlandskAdresse: Any?,
    ) : Livshendelse() {
        override val opplysningsType = Opplysningstype.BOSTEDSADRESSE_V1
    }

    data class Kontaktadresse(
        override val personidenter: List<String>,
        val utenlandskAdresse: Any?,
    ) : Livshendelse() {
        override val opplysningsType = Opplysningstype.KONTAKTADRESSE_V1
    }

    data class Adressebeskyttelse(
        override val personidenter: List<String>,
        val adressebeskyttelse: Gradering,
    ) : Livshendelse() {
        override val opplysningsType = Opplysningstype.ADRESSEBESKYTTELSE_V1
    }
}

enum class Gradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

enum class Opplysningstype {
    BOSTEDSADRESSE_V1,
    KONTAKTADRESSE_V1,
    ADRESSEBESKYTTELSE_V1,
}
