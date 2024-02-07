package no.nav.utenlandsadresser.app

import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.FeedRepository

class FeedService(
    private val feedRepository: FeedRepository,
    private val abonnementService: AbonnementService,
) {
    fun nyMelding(identitetsnummer: List<Identitetsnummer>) {
        // Hent alle client id-er som er abonnert på identitetsnummer
        //val clientIds = abonnementService.hentAbonnementer()

        // Lagre melding for hver client id med økende løpenummer
    }
}