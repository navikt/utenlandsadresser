package no.nav.utenlandsadresser.application.port.inbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

/**
 * Starter abonnement på postadresse. Har personen utenlandsk postadresse, legges det en hendelse på feeden.
 */
interface StartAbonnement {
    suspend fun start(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StartAbonnementError, Abonnement>
}

sealed class StartAbonnementError {
    data class AbonnementFinnesAllerede(
        val abonnement: Abonnement,
    ) : StartAbonnementError()

    data object KunneIkkeHentePostadresse : StartAbonnementError()
}
