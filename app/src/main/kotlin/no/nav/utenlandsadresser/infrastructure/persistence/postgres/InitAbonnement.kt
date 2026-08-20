package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.utenlandsadresser.app.AbonnementInitializer
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.CreateAbonnementError
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class PostgresAbonnementInitializer(
    private val abonnementRepository: PostgresAbonnementRepository,
    private val feedRepository: PostgresFeedRepository,
    private val database: R2dbcDatabase,
) : AbonnementInitializer {
    /**
     * Oppretter et abonnement i databasen hvis det ikke allerede finnes.
     * Hvis et abonnement allerede finnes, så returneres det eksisterende abonnementet.
     *
     * Uansett om abonnementet finnes eller ikke, så opprettes et feed event hvis det finnes en utenlandsk adresse.
     * Dette er for å forsikre at alle abonnementer får en feed event når de opprettes.
     */
    override suspend fun initAbonnement(
        abonnement: Abonnement,
        postadresse: Postadresse?,
    ): Either<InitAbonnementError, Abonnement> =
        suspendTransaction(db = database, readOnly = false) {
            val createAbonnementResult =
                abonnementRepository.createAbonnement(abonnement).mapLeft {
                    when (it) {
                        is CreateAbonnementError.AlreadyExists -> {
                            InitAbonnementError.AbonnementAlreadyExists(it.abonnement)
                        }
                    }
                }

            val abonnementFromRepository = createAbonnementResult.getOrElse { it.abonnement }

            if (postadresse is Postadresse.Utenlandsk) {
                val feedEvent =
                    FeedEvent.Incoming(
                        identitetsnummer = abonnement.identitetsnummer,
                        abonnementId = abonnementFromRepository.id,
                        organisasjonsnummer = abonnement.organisasjonsnummer,
                        hendelsestype = Hendelsestype.OppdatertAdresse,
                    )
                feedRepository.createFeedEvent(feedEvent)
            }

            createAbonnementResult
        }
}

sealed class InitAbonnementError {
    data class AbonnementAlreadyExists(
        val abonnement: Abonnement,
    ) : InitAbonnementError()
}
