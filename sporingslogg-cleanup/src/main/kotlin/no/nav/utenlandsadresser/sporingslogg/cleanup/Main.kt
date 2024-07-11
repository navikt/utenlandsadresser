package no.nav.utenlandsadresser.sporingslogg.cleanup

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

fun main() {
}

private val Int.years: Duration
    get() = (this * 365).days
