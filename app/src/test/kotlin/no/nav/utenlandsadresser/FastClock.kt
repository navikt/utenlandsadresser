package no.nav.utenlandsadresser

import kotlin.time.Clock
import kotlin.time.Instant

/** Klokke som står stille, så testene kan sjekke tidspunktene som lagres. */
class FastClock(
    var nå: Instant = Instant.parse("2025-01-01T12:00:00Z"),
) : Clock {
    override fun now(): Instant = nå
}
