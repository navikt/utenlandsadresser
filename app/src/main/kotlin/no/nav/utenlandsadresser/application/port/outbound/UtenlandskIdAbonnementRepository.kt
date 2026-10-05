package no.nav.utenlandsadresser.application.port.outbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import kotlin.uuid.Uuid

interface UtenlandskIdAbonnementRepository {
    suspend fun slettAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<SlettAbonnementError, Unit>

    /**
     * Sjekker at abonnementet finnes og tilhører mottakeren.
     */
    suspend fun finnesAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Boolean
}
