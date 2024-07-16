package no.nav.utenlandsadresser.infrastructure.kafka.avro

import com.github.avrokotlin.avro4k.Avro
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.kafka.GraderingAvro
import no.nav.utenlandsadresser.infrastructure.kafka.Livshendelse
import no.nav.utenlandsadresser.infrastructure.kafka.Opplysningstype
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

@Serializable
data class LivshendelseAvro(
    val personidenter: List<String>,
    val opplysningstype: String,
    val adressebeskyttelse: AdressebeskyttelseAvro?,
) : GenericRecord {
    fun toDomain(): Livshendelse? {
        val personidenter = personidenter.map(::Identitetsnummer)
        val opplysningstype = Opplysningstype.entries.firstOrNull { it.name == opplysningstype.trim() }
        return when (opplysningstype) {
            Opplysningstype.BOSTEDSADRESSE_V1 ->
                Livshendelse.Bostedsadresse(
                    personidenter = personidenter,
                )

            Opplysningstype.KONTAKTADRESSE_V1 ->
                Livshendelse.Kontaktadresse(
                    personidenter = personidenter,
                )

            Opplysningstype.ADRESSEBESKYTTELSE_V1 ->
                Livshendelse.Adressebeskyttelse(
                    personidenter = personidenter,
                    // Om adressebeskyttelse er null så tyder det på at adressebeskyttelsen er fjernet
                    adressebeskyttelse =
                        adressebeskyttelse?.gradering
                            ?: GraderingAvro.UGRADERT,
                )

            null -> null
        }
    }

    override fun getSchema(): Schema = Avro.default.schema(serializer())

    override fun put(
        key: String?,
        v: Any?,
    ): Unit = throw UnsupportedOperationException()

    override fun put(
        i: Int,
        v: Any?,
    ): Unit = throw UnsupportedOperationException()

    override fun get(key: String?): Any? =
        when (key) {
            "personidenter" -> personidenter
            "opplysningstype" -> opplysningstype
            "adressebeskyttelse" -> adressebeskyttelse
            else -> throw IllegalArgumentException("Unknown key: $key")
        }

    override fun get(i: Int): Any? =
        when (i) {
            0 -> personidenter
            1 -> opplysningstype
            2 -> adressebeskyttelse
            else -> throw IllegalArgumentException("Unknown index: $i")
        }
}
