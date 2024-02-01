package no.nav.utenlandsadresser.domain

import kotlinx.datetime.Instant

data class Abonnement(
    val clientId: ClientId,
    val fødselsnummer: Fødselsnummer,
    val løpenummer: Int,
    val opprettet: Instant,
)