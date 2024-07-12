package no.nav.utenlandsadresser.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

val Int.years: Duration
    get() = (this * 365).days