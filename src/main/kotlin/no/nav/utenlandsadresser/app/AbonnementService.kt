package no.nav.utenlandsadresser.app

import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Identitetsnummer

class AbonnementService(
    private val abbonementRepository: AbonnementRepository,
) {
    fun startAbonnement(identitetsnummer: Identitetsnummer, clientId: ClientId): Abonnement {
        val abonnement = Abonnement(
            clientId = clientId,
            identitetsnummer = identitetsnummer,
            opprettet = Clock.System.now(),
        )

        abbonementRepository.createAbonnement(abonnement)

        return abonnement
    }

    fun stoppAbonnement(identitetsnummer: Identitetsnummer, clientId: ClientId) {
        abbonementRepository.deleteAbonnement(identitetsnummer, clientId)
    }

    fun hentAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> {
        return abbonementRepository.getAbonnementer(identitetsnummer)
    }
}