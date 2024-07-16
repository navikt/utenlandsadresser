package no.nav.utenlandsadresser.infrastructure.kafka.avro

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroConfiguration
import org.apache.kafka.common.serialization.Serializer

class Avro4kLivshendelseSerializer : Serializer<LivshendelseAvro> {
    private val avro = Avro(AvroConfiguration(implicitNulls = true))

    override fun serialize(
        topic: String?,
        data: LivshendelseAvro,
    ): ByteArray = avro.encodeToByteArray(LivshendelseAvro.serializer(), data)
}
