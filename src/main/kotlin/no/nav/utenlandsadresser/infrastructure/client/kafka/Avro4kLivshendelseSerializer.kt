package no.nav.utenlandsadresser.infrastructure.client.kafka

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroConfiguration
import org.apache.kafka.common.serialization.Serializer

class Avro4kLivshendelseSerializer : Serializer<LivshendelseAvro> {
    private val avro = Avro(AvroConfiguration(implicitNulls = true))
    override fun serialize(topic: String?, data: LivshendelseAvro): ByteArray =
        avro.encodeToByteArray(LivshendelseAvro.serializer(), data)
}