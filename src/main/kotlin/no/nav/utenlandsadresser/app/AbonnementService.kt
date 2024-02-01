package no.nav.utenlandsadresser.app

import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.database.AbonnementRepository
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Fødselsnummer

class AbonnementService(
    private val abbonementRepository: AbonnementRepository,
) {
    fun startAbonnement(fødselsnummer: Fødselsnummer, clientId: ClientId): Abonnement {
        val abonnement = Abonnement(
            fødselsnummer = fødselsnummer,
            clientId = clientId,
            løpenummer = 0,
            opprettet = Clock.System.now(),
        )

        abbonementRepository.createAbonnement(abonnement)

        return abonnement
    }

    fun stoppAbonnement(fødselsnummer: Fødselsnummer, clientId: ClientId) {
        abbonementRepository.deleteAbonnement(fødselsnummer, clientId)
    }
}