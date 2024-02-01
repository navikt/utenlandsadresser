package no.nav.utenlandsadresser.database.dto

import arrow.core.getOrElse
import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Fødselsnummer

data class AbonnementDto(
    val clientId: String,
    val fødselsnummer: String,
    val løpenummer: Int,
    val opprettet: Instant,
) {
    fun toDomain(): Abonnement = Abonnement(
        clientId = ClientId(clientId),
        fødselsnummer = Fødselsnummer(fødselsnummer).getOrElse {
            throw IllegalArgumentException("Invalid fødselsnummer")
        },
        løpenummer = løpenummer,
        opprettet = opprettet,
    )

    companion object {
        fun fromDomain(abonnement: Abonnement): AbonnementDto =
            AbonnementDto(
                clientId = abonnement.clientId.value,
                fødselsnummer = abonnement.fødselsnummer.value,
                løpenummer = abonnement.løpenummer,
                opprettet = abonnement.opprettet,
            )
    }
}