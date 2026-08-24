package no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto

import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.PostgresAbonnementRepository
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class AbonnementDto(
    val id: Uuid,
    val organisasjonsnummer: String,
    val identitetsnummer: String,
    val opprettet: Instant,
) {
    fun toDomain(): Abonnement =
        Abonnement(
            id = id,
            organisasjonsnummer = Organisasjonsnummer(organisasjonsnummer),
            identitetsnummer = Identitetsnummer(identitetsnummer),
            opprettet = opprettet,
        )

    companion object {
        fun fromDomain(abonnement: Abonnement): AbonnementDto =
            AbonnementDto(
                id = abonnement.id,
                organisasjonsnummer = abonnement.organisasjonsnummer.value,
                identitetsnummer = abonnement.identitetsnummer.value,
                opprettet = abonnement.opprettet,
            )

        fun PostgresAbonnementRepository.fromRow(row: ResultRow): AbonnementDto =
            AbonnementDto(
                id = row[idColumn],
                organisasjonsnummer = row[organisasjonsnummerColumn],
                identitetsnummer = row[identitetsnummerColumn],
                opprettet = row[opprettetColumn],
            )
    }
}
