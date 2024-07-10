package no.nav.utenlandsadresser.domain

import kotlinx.datetime.Instant
import java.util.*

data class Abonnement(
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val identitetsnummer: Identitetsnummer,
    val opprettet: Instant,
)