package no.nav.utenlandsadresser.infrastructure.client.kafka

import arrow.core.getOrElse
import no.nav.utenlandsadresser.domain.Identitetsnummer
import org.apache.avro.generic.GenericRecord

sealed class Livshendelse {
    abstract val personidenter: List<Identitetsnummer>

    companion object {
        fun from(genericRecord: GenericRecord): Livshendelse? {
            val genericPersonidenter = (genericRecord["personidenter"] as? List<*>)
                ?: return null
            val personidenter = genericPersonidenter
                .map { it.toString() }
                .map {
                    Identitetsnummer(it)
                        .getOrElse { error -> throw IllegalArgumentException("Invalid identitetsnummer: $error") }
                }


            val recordOpplysningstype = (genericRecord["opplysningstype"]?.toString())
                ?: return null
            val opplysningstype = Opplysningstype.entries.firstOrNull { it.name == recordOpplysningstype.trim() }

            return when (opplysningstype) {
                Opplysningstype.BOSTEDSADRESSE_V1 -> Bostedsadresse(
                    personidenter = personidenter,
                )

                Opplysningstype.KONTAKTADRESSE_V1 -> Kontaktadresse(
                    personidenter = personidenter,
                )

                Opplysningstype.ADRESSEBESKYTTELSE_V1 -> {
                    val adressebeskyttelse = genericRecord["adressebeskyttelse"] as? GenericRecord
                        ?: return Adressebeskyttelse(
                            personidenter = personidenter,
                            adressebeskyttelse = Gradering.UGRADERT,
                        )
                    val gradering = adressebeskyttelse["gradering"].toString()
                    Adressebeskyttelse(
                        personidenter = personidenter,
                        adressebeskyttelse = Gradering.valueOf(gradering.trim()),
                    )
                }

                null -> null
            }
        }
    }

    data class Bostedsadresse(
        override val personidenter: List<Identitetsnummer>,
    ) : Livshendelse()

    data class Kontaktadresse(
        override val personidenter: List<Identitetsnummer>,
    ) : Livshendelse()

    data class Adressebeskyttelse(
        override val personidenter: List<Identitetsnummer>,
        val adressebeskyttelse: Gradering,
    ) : Livshendelse()
}