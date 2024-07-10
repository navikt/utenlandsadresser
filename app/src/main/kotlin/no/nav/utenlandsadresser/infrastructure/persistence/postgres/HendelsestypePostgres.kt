package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.Hendelsestype

enum class HendelsestypePostgres {
    ADRESSEBESKYTTELSE, OPPDATERT_ADRESSE;


    companion object {
        fun fromDomain(hendelsestype: Hendelsestype): HendelsestypePostgres = when (hendelsestype) {
            is Hendelsestype.Adressebeskyttelse -> fromGradering(hendelsestype.gradering)
            Hendelsestype.OppdatertAdresse -> OPPDATERT_ADRESSE
        }

        private fun fromGradering(gradering: AdressebeskyttelseGradering): HendelsestypePostgres = when (gradering) {
            AdressebeskyttelseGradering.GRADERT -> ADRESSEBESKYTTELSE
            AdressebeskyttelseGradering.UGRADERT -> OPPDATERT_ADRESSE
        }
    }

    fun toDomain(): Hendelsestype = when (this) {
        ADRESSEBESKYTTELSE -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
        OPPDATERT_ADRESSE -> Hendelsestype.OppdatertAdresse
    }
}
