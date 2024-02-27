package no.nav.utenlandsadresser.infrastructure.client.kafka

import org.apache.avro.generic.GenericRecord

sealed class Livshendelse {
    abstract val personidenter: List<String>

    companion object {
        fun from(genericRecord: GenericRecord): Livshendelse? {
            val personidenter = (genericRecord["personidenter"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val opplysningstype = Opplysningstype.entries.firstOrNull { it.name == genericRecord["opplysningstype"] }
            return when (opplysningstype) {
                Opplysningstype.BOSTEDSADRESSE_V1 -> Bostedsadresse(
                    personidenter = personidenter,
                )

                Opplysningstype.KONTAKTADRESSE_V1 -> Kontaktadresse(
                    personidenter = personidenter,
                )

                Opplysningstype.ADRESSEBESKYTTELSE_V1 -> Adressebeskyttelse(
                    personidenter = personidenter,
                    adressebeskyttelse = Gradering.valueOf(genericRecord["adressebeskyttelse"].toString()),
                )

                null -> null
            }
        }
    }

    data class Bostedsadresse(
        override val personidenter: List<String>,
    ) : Livshendelse()

    data class Kontaktadresse(
        override val personidenter: List<String>,
    ) : Livshendelse()

    data class Adressebeskyttelse(
        override val personidenter: List<String>,
        val adressebeskyttelse: Gradering,
    ) : Livshendelse()
}