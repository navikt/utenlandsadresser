package no.nav.utenlandsadresser.infrastructure.kafka

import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.Hendelsestype

enum class GraderingAvro {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT;

    fun toDomain(): Hendelsestype = when (this) {
        STRENGT_FORTROLIG_UTLAND -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
        STRENGT_FORTROLIG -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
        FORTROLIG -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
        UGRADERT -> Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT)
    }
}