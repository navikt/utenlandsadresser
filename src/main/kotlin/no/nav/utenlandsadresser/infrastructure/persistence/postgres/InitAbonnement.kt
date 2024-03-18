package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import kotlinx.coroutines.Dispatchers
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
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
    ): Either<InitAbonnementError, Abonnement> =
        /*
        Tilgjengeligjør repositoryene i konteksten for å kunne bruke
        funksjonene som extender transaction.
        */
        with(abonnementRepository) {
            with(feedRepository) {
                newSuspendedTransaction(Dispatchers.IO) {
                    either {
                        val createdAbonnement = createAbonnement(abonnement).getOrElse {
                            when (it) {
                                is CreateAbonnementError.AlreadyExists -> raise(
                                    InitAbonnementError.AbonnementAlreadyExists(it.abonnement)
                                )
                            }
                        }

                        if (postadresse is Postadresse.Utenlandsk) {
                            val feedEvent = FeedEvent.Incoming(
                                identitetsnummer = abonnement.identitetsnummer,
                                abonnementId = createdAbonnement.id,
                                organisasjonsnummer = abonnement.organisasjonsnummer,
                                hendelsestype = Hendelsestype.OppdatertAdresse,
                            )
                            createFeedEvent(feedEvent)
                        }

                        createdAbonnement
                    }.onLeft {
                        rollback()
                    }
                }
            }
        }
}

sealed class InitAbonnementError {
    data class AbonnementAlreadyExists(val abonnement: Abonnement) : InitAbonnementError()
}