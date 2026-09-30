package no.nav.utenlandsadresser.app

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.infrastructure.persistence.DeleteAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.InitAbonnementError
import kotlin.uuid.Uuid

interface UtenlandskIdAbonnementRepository {
    suspend fun deleteAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<DeleteAbonnementError, Unit>

    /**
     * Sjekker at abonnementet finnes og tilhører mottakeren.
     */
    suspend fun finnesAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Boolean
}

/**
 * Oppretter et abonnement på utenlandsk id og eventuelt en feed-hendelse. Implementasjonen må
 * passe på at databaseoperasjonene blir utført innenfor én transaksjon.
 */
interface UtenlandskIdAbonnementInitializer {
    suspend fun initAbonnement(
        abonnement: Abonnement,
        harUtenlandskId: Boolean,
    ): Either<InitAbonnementError, Abonnement>
}

interface UtenlandskIdFeedRepository {
    suspend fun getFeedEvent(
        organisasjonsnummer: Organisasjonsnummer,
        løpenummer: Løpenummer,
    ): UtenlandskIdFeedEvent.Outgoing?
}
