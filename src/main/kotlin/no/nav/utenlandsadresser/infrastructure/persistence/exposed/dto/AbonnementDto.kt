package no.nav.utenlandsadresser.infrastructure.persistence.exposed.dto

import arrow.core.getOrElse
import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.AbonnementExposedRepository
import org.jetbrains.exposed.sql.ResultRow

data class AbonnementDto(
    val clientId: String,
    val fødselsnummer: String,
    val opprettet: Instant,
) {
    fun toDomain(): Abonnement = Abonnement(
        clientId = ClientId(clientId),
        identitetsnummer = Identitetsnummer(fødselsnummer).getOrElse {
            throw IllegalArgumentException("Invalid fødselsnummer")
        },
        opprettet = opprettet,
    )

    companion object {
        fun fromDomain(abonnement: Abonnement): AbonnementDto =
            AbonnementDto(
                clientId = abonnement.clientId.value,
                fødselsnummer = abonnement.identitetsnummer.value,
                opprettet = abonnement.opprettet,
            )

        context(AbonnementExposedRepository)
        fun fromRow(row: ResultRow): AbonnementDto = AbonnementDto(
            clientId = row[clientIdColumn],
            fødselsnummer = row[identitetsnummerColumn],
            opprettet = row[opprettetColumn],
        )
    }
}