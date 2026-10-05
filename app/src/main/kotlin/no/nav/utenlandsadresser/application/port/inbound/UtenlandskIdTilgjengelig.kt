package no.nav.utenlandsadresser.application.port.inbound

/**
 * Om utenlandsk id er tilgjengelig for konsumentene. Er den ikke det, skal endepunktene oppføre seg som om de ikke finnes.
 *
 * Svaret kan endre seg mens appen kjører, så det må sjekkes ved hvert kall.
 */
fun interface UtenlandskIdTilgjengelig {
    fun erTilgjengelig(): Boolean
}
