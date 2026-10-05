package no.nav.utenlandsadresser.domain

/**
 * En endring i Folkeregisteret som kan påvirke postadressen til en person med abonnement.
 */
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
        val gradering: Gradering,
    ) : Livshendelse() {
        /** Graderingene i Folkeregisteret. */
        enum class Gradering {
            STRENGT_FORTROLIG_UTLAND,
            STRENGT_FORTROLIG,
            FORTROLIG,
            UGRADERT,
        }
    }
}
