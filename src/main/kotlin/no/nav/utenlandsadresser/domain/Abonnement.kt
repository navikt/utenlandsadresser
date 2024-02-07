package no.nav.utenlandsadresser.domain

import kotlinx.datetime.Instant

data class Abonnement(
    val clientId: ClientId,
    val identitetsnummer: Identitetsnummer,
    val opprettet: Instant,
)