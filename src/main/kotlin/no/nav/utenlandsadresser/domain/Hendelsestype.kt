package no.nav.utenlandsadresser.domain

sealed class Hendelsestype {
    data object OppdatertAdresse : Hendelsestype()
    data class Adressebeskyttelse(val gradering: AdressebeskyttelseGradering) : Hendelsestype()
}
