package no.nav.utenlandsadresser.infrastructure.kafka

import no.nav.utenlandsadresser.domain.Identitetsnummer

sealed class Livshendelse {
    abstract val personidenter: List<Identitetsnummer>

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