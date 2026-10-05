package no.nav.utenlandsadresser.application.port.outbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import kotlin.uuid.Uuid

interface AbonnementRepository {
    suspend fun opprettAbonnement(abonnement: Abonnement): Either<OpprettAbonnementError, Abonnement>

    suspend fun slettAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<SlettAbonnementError, Unit>

    /**
     * Henter alle abonnementer på noen av identitetsnumrene. En person kan ha flere identer, for eksempel
     * både fødselsnummer og D-nummer.
     */
    suspend fun hentAbonnementer(identitetsnummer: List<Identitetsnummer>): List<Abonnement>

    /**
     * Sjekker at abonnementet finnes og tilhører mottakeren.
     */
    suspend fun finnesAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Boolean
}

sealed class OpprettAbonnementError {
    data class FinnesAllerede(
        val abonnement: Abonnement,
    ) : OpprettAbonnementError()
}

sealed class SlettAbonnementError {
    data object IkkeFunnet : SlettAbonnementError()
}
