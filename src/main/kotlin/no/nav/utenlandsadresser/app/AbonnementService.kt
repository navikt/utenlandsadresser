package no.nav.utenlandsadresser.app

import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Identitetsnummer

class AbonnementService(
    private val abbonementRepository: AbonnementRepository,
) {
    fun startAbonnement(identitetsnummer: Identitetsnummer, organisasjonsnummer: Organisasjonsnummer): Abonnement {
        val abonnement = Abonnement(
            organisasjonsnummer = organisasjonsnummer,
            identitetsnummer = identitetsnummer,
            opprettet = Clock.System.now(),
        )

        abbonementRepository.createAbonnement(abonnement)

        return abonnement
    }

    fun stoppAbonnement(identitetsnummer: Identitetsnummer, organisasjonsnummer: Organisasjonsnummer) {
        abbonementRepository.deleteAbonnement(identitetsnummer, organisasjonsnummer)
    }

    fun hentAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> {
        return abbonementRepository.getAbonnementer(identitetsnummer)
    }
}