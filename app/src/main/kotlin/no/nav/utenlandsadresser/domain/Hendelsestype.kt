package no.nav.utenlandsadresser.domain

sealed class Hendelsestype {
    data object OppdatertAdresse : Hendelsestype()

    data class Adressebeskyttelse(
        val gradering: AdressebeskyttelseGradering,
    ) : Hendelsestype()

    /**
     * Om adressen skal holdes tilbake. Uttømmende `when`, så en ny gradering ikke kan legges til uten at noen
     * tar stilling til den her.
     */
    val erGradert: Boolean
        get() =
            when (this) {
                is Adressebeskyttelse -> {
                    when (gradering) {
                        AdressebeskyttelseGradering.GRADERT -> true
                        AdressebeskyttelseGradering.UGRADERT -> false
                    }
                }

                OppdatertAdresse -> {
                    false
                }
            }
}
