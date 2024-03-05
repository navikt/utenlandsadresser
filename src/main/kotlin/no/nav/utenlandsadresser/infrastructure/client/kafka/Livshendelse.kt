package no.nav.utenlandsadresser.infrastructure.client.kafka

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Identitetsnummer

@Serializable
data class AdressebeskyttelseAvro(
    val gradering: String,
)

@Serializable
data class LivshendelseAvro(
    val personidenter: List<String>,
    val opplysningstype: String,
    val adressebeskyttelse: AdressebeskyttelseAvro?,
)

sealed class Livshendelse {
    abstract val personidenter: List<Identitetsnummer>

    companion object {
        fun from(livshendelseAvro: LivshendelseAvro): Livshendelse? {
            val personidenter = livshendelseAvro.personidenter
                .map {
                    Identitetsnummer(it)
                }

            val opplysningstype =
                Opplysningstype.entries.firstOrNull { it.name == livshendelseAvro.opplysningstype.trim() }

            return when (opplysningstype) {
                Opplysningstype.BOSTEDSADRESSE_V1 -> Bostedsadresse(
                    personidenter = personidenter,
                )

                Opplysningstype.KONTAKTADRESSE_V1 -> Kontaktadresse(
                    personidenter = personidenter,
                )

                Opplysningstype.ADRESSEBESKYTTELSE_V1 -> {
                    val adressebeskyttelse = livshendelseAvro.adressebeskyttelse
                        ?: return Adressebeskyttelse(
                            personidenter = personidenter,
                            adressebeskyttelse = Gradering.UGRADERT,
                        )
                    val gradering = adressebeskyttelse.gradering
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