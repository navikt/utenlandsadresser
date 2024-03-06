package no.nav.utenlandsadresser.infrastructure.client.kafka.avro

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroConfiguration
import com.github.avrokotlin.avro4k.io.AvroDecodeFormat
import org.apache.kafka.common.serialization.Deserializer

class Avro4kLivshendelseDeserializer : Deserializer<LivshendelseAvro> {
    private val avro = Avro(AvroConfiguration(implicitNulls = true))
    override fun deserialize(topic: String?, data: ByteArray): LivshendelseAvro {
        return avro.openInputStream(LivshendelseAvro.serializer()) {
            decodeFormat = AvroDecodeFormat.Binary(avro.schema(LivshendelseAvro.serializer()))
        }
            .from(data)
            .nextOrThrow()
    }
}
