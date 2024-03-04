package no.nav.utenlandsadresser.infrastructure.client.kafka

import com.github.avrokotlin.avro4k.Avro
import kotlinx.serialization.KSerializer
import org.apache.kafka.common.serialization.Deserializer

class Avro4kDeserializer<T>(private val avro: Avro, private val serializer: KSerializer<T>) : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray): T = avro.decodeFromByteArray(serializer, data)
}