package no.nav.utenlandsadresser.infrastructure.client.kafka

import org.apache.avro.generic.GenericRecord
import org.slf4j.Logger

sealed class Livshendelse {
    abstract val personidenter: List<String>

    companion object {
        context(Logger)
        fun from(genericRecord: GenericRecord): Livshendelse? {
            val genericPersonidenter = (genericRecord["personidenter"] as? List<*>)
                ?: run {
                    warn("Received message without personidenter: $genericRecord")
                    return null
                }
            val personidenter = genericPersonidenter.map { it.toString() }

            val recordOpplysningstype = (genericRecord["opplysningstype"]?.toString())
                ?: run {
                    warn("Received message without opplysningstype: $genericRecord")
                    return null
                }
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
                        ?: run {
                            warn("Received message without adressebeskyttelse: $genericRecord")
                            return null
                        }
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