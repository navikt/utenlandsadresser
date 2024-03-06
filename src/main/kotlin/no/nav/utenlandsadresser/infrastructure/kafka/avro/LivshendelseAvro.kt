package no.nav.utenlandsadresser.infrastructure.kafka.avro

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.kafka.Gradering
import no.nav.utenlandsadresser.infrastructure.kafka.Livshendelse
import no.nav.utenlandsadresser.infrastructure.kafka.Opplysningstype

@Serializable
data class LivshendelseAvro(
    val personidenter: List<String>,
    val opplysningstype: String,
    val adressebeskyttelse: AdressebeskyttelseAvro?,
) {
    fun toDomain(): Livshendelse? {
        val personidenter = personidenter.map(::Identitetsnummer)
        val opplysningstype = Opplysningstype.entries.firstOrNull { it.name == opplysningstype.trim() }
        return when (opplysningstype) {
            Opplysningstype.BOSTEDSADRESSE_V1 -> Livshendelse.Bostedsadresse(
                personidenter = personidenter,
            )
            Opplysningstype.KONTAKTADRESSE_V1 -> Livshendelse.Kontaktadresse(
                personidenter = personidenter,
            )
            Opplysningstype.ADRESSEBESKYTTELSE_V1 -> {
                val adressebeskyttelse = adressebeskyttelse
                    ?: return Livshendelse.Adressebeskyttelse(
                        personidenter = personidenter,
                        adressebeskyttelse = Gradering.UGRADERT,
                    )
                val gradering = adressebeskyttelse.gradering
                Livshendelse.Adressebeskyttelse(
                    personidenter = personidenter,
                    adressebeskyttelse = Gradering.valueOf(gradering.trim()),
                )
            }
            null -> null
        }
    }
}