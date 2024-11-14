package no.nav.utenlandsadresser.infrastructure.kafka

import com.github.avrokotlin.avro4k.Avro
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.Hendelsestype
import org.apache.avro.Schema
import org.apache.avro.generic.GenericEnumSymbol

@Serializable
enum class GraderingAvro : GenericEnumSymbol<GraderingAvro> {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
    ;

    override fun getSchema(): Schema = Avro.schema(serializer().descriptor)

    fun toDomain(): Hendelsestype =
        when (this) {
            STRENGT_FORTROLIG_UTLAND -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
            STRENGT_FORTROLIG -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
            FORTROLIG -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
            UGRADERT -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT)
        }
}
