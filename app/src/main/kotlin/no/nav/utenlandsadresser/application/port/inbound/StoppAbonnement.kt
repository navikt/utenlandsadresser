package no.nav.utenlandsadresser.application.port.inbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import kotlin.uuid.Uuid

/**
 * Stopper abonnement på postadresse. Hendelsene blir liggende på feeden.
 */
interface StoppAbonnement {
    suspend fun stopp(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StoppAbonnementError, Unit>
}

sealed class StoppAbonnementError {
    data object AbonnementIkkeFunnet : StoppAbonnementError()
}
