package no.nav.utenlandsadresser.application.port.inbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer

/**
 * Starter abonnement på utenlandsk id. Har personen utenlandsk id, legges det en hendelse på feeden.
 * Feeden lagrer ikke selve id-en.
 */
interface StartUtenlandskIdAbonnement {
    suspend fun start(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StartUtenlandskIdAbonnementError, Abonnement>
}

sealed class StartUtenlandskIdAbonnementError {
    data class AbonnementFinnesAllerede(
        val abonnement: Abonnement,
    ) : StartUtenlandskIdAbonnementError()

    data object KunneIkkeHenteUtenlandskId : StartUtenlandskIdAbonnementError()
}
