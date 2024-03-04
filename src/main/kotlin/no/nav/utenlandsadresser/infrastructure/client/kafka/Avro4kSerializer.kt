package no.nav.utenlandsadresser.infrastructure.client.kafka

import com.github.avrokotlin.avro4k.Avro
import kotlinx.serialization.KSerializer
import org.apache.kafka.common.serialization.Serializer

class Avro4kSerializer<T>(private val avro: Avro, private val serializer: KSerializer<T>) : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray = avro.encodeToByteArray(serializer, data)
}