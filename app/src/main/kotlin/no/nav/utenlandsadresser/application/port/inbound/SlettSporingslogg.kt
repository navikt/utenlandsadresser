package no.nav.utenlandsadresser.application.port.inbound

import arrow.core.Either
import kotlin.time.Duration

interface SlettSporingslogg {
    /**
     * Sletter sporingslogger med tidspunkt for utlevering eldre enn [alder] regnet fra nå.
     *
     * [alder] må være positiv. Null eller negativ alder ville gitt en grense nå eller frem i tid, og slettet hele loggen.
     */
    suspend fun slettEldreEnn(alder: Duration): Either<SlettSporingsloggError, Unit>
}

sealed class SlettSporingsloggError {
    data object AlderIkkePositiv : SlettSporingsloggError()
}
