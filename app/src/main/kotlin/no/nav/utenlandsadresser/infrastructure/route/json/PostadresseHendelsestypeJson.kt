package no.nav.utenlandsadresser.infrastructure.route.json

import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.Hendelsestype

enum class PostadresseHendelsestypeJson {
    OPPDATERT_ADRESSE,
    SLETTET_ADRESSE,
    ;

    companion object {
        fun fromDomain(hendelsestype: Hendelsestype): PostadresseHendelsestypeJson =
            when (hendelsestype) {
                Hendelsestype.OppdatertAdresse -> OPPDATERT_ADRESSE
                is Hendelsestype.Adressebeskyttelse -> fromGradering(hendelsestype.gradering)
            }

        private fun fromGradering(gradering: AdressebeskyttelseGradering): PostadresseHendelsestypeJson =
            when (gradering) {
                AdressebeskyttelseGradering.GRADERT -> SLETTET_ADRESSE
                AdressebeskyttelseGradering.UGRADERT -> OPPDATERT_ADRESSE
            }
    }
}
