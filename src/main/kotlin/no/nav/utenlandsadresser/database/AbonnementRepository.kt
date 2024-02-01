package no.nav.utenlandsadresser.database

import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Fødselsnummer

interface AbonnementRepository {

    fun createAbonnement(abonnement: Abonnement)
    fun deleteAbonnement(fødselsnummer: Fødselsnummer, clientId: ClientId)
}
