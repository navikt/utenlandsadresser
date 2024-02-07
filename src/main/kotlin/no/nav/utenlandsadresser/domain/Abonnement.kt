package no.nav.utenlandsadresser.domain

import kotlinx.datetime.Instant

data class Abonnement(
    val organisasjonsnummer: Organisasjonsnummer,
    val identitetsnummer: Identitetsnummer,
    val opprettet: Instant,
)