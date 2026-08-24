package no.nav.utenlandsadresser.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Abonnement(
    val id: Uuid,
    val organisasjonsnummer: Organisasjonsnummer,
    val identitetsnummer: Identitetsnummer,
    val opprettet: Instant,
)
