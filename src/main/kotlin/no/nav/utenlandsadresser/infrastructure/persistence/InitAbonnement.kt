package no.nav.utenlandsadresser.infrastructure.persistence

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.InitAbonnementError

interface InitAbonnement {
    suspend fun initAbonnement(abonnement: Abonnement, postadresse: Postadresse?): Either<InitAbonnementError, Unit>
}