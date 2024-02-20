package no.nav.utenlandsadresser.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import kotlinx.coroutines.Dispatchers
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.CreateAbonnementError
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class PostgresAbonnementInitializer(
    private val abonnementRepository: AbonnementPostgresRepository,
    private val feedRepository: FeedPostgresRepository,
) : AbonnementInitializer {
    override suspend fun initAbonnement(
        abonnement: Abonnement,
        postadresse: Postadresse?
    ): Either<InitAbonnementError, Unit> =
        /*
        Tilgjengeligjør repositoryene i konteksten for å kunne bruke
        funksjonene som extender transaction.
        */
        with(abonnementRepository) {
            with(feedRepository) {
                newSuspendedTransaction(Dispatchers.IO) {
                    either {
                        createAbonnement(abonnement).getOrElse {
                            when (it) {
                                CreateAbonnementError.AlreadyExists -> raise(InitAbonnementError.AbonnementAlreadyExists)
                            }
                        }

                        if (postadresse is Postadresse.Utenlandsk) {
                            val feedEvent =
                                FeedEvent.Incoming(abonnement.identitetsnummer, abonnement.organisasjonsnummer)
                            createFeedEvent(feedEvent)
                        }
                    }.onLeft {
                        rollback()
                    }
                }
            }
        }
}

sealed class InitAbonnementError {
    data object AbonnementAlreadyExists : InitAbonnementError()
}