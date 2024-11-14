package no.nav.utenlandsadresser.infrastructure.kafka.avro

import com.github.avrokotlin.avro4k.Avro
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.infrastructure.kafka.GraderingAvro
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

@Serializable
data class AdressebeskyttelseAvro(
    val gradering: GraderingAvro?,
) : GenericRecord {
    override fun getSchema(): Schema = Avro.schema(serializer().descriptor)

    override fun put(
        key: String?,
        v: Any?,
    ) = throw UnsupportedOperationException()

    override fun put(
        i: Int,
        v: Any?,
    ) = throw UnsupportedOperationException()

    override fun get(key: String?): Any? =
        when (key) {
            "gradering" -> gradering
            else -> throw NoSuchElementException("No such element: $key")
        }

    override fun get(i: Int): Any? =
        when (i) {
            0 -> gradering
            else -> throw NoSuchElementException("No such element: $i")
        }
}
