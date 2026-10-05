package no.nav.utenlandsadresser.application.port.inbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import kotlin.uuid.Uuid

/**
 * Stopper abonnement på utenlandsk id. Hendelsene blir liggende på feeden.
 */
interface StoppUtenlandskIdAbonnement {
    suspend fun stopp(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StoppAbonnementError, Unit>
}
