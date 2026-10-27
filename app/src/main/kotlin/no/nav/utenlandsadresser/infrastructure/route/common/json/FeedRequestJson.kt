package no.nav.utenlandsadresser.infrastructure.route.common.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class FeedRequestJson(
    @Serializable(with = LøpenummerSomStrengSerializer::class)
    val løpenummer: Int,
)

/**
 * Løpenummer sendes som streng, for eksempel `"1"`. Er strengen ikke et heltall, feiler deserialiseringen,
 * og Ktor svarer 400.
 */
internal object LøpenummerSomStrengSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("no.nav.utenlandsadresser.LøpenummerSomStreng", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Int,
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Int =
        decoder.decodeString().toIntOrNull() ?: throw SerializationException("Løpenummer må være et heltall")
}
