package no.nav.utenlandsadresser.application.port.inbound

import kotlin.time.Duration

interface SlettSporingslogg {
    /** Sletter sporingslogger med tidspunkt for utlevering eldre enn [alder] regnet fra nå. */
    suspend fun slettEldreEnn(alder: Duration)
}
