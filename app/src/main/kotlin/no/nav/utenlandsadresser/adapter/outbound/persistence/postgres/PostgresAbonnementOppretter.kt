package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.utenlandsadresser.application.port.outbound.AbonnementOppretter
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementMedEventError
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Postadresse
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class PostgresAbonnementOppretter(
    private val abonnementRepository: PostgresAbonnementRepository,
    private val feedRepository: PostgresFeedRepository,
    private val database: R2dbcDatabase,
) : AbonnementOppretter {
    /**
     * Oppretter et abonnement i databasen hvis det ikke allerede finnes.
     * Hvis et abonnement allerede finnes, så returneres det eksisterende abonnementet.
     *
     * Uansett om abonnementet finnes eller ikke, så opprettes et feed event hvis det finnes en utenlandsk adresse.
     * Dette er for å forsikre at alle abonnementer får en feed event når de opprettes.
     */
    override suspend fun opprettMedEvent(
        abonnement: Abonnement,
        postadresse: Postadresse?,
    ): Either<OpprettAbonnementMedEventError, Abonnement> =
        suspendTransaction(db = database, readOnly = false) {
            val createAbonnementResult =
                abonnementRepository.opprettAbonnement(abonnement).mapLeft {
                    when (it) {
                        is OpprettAbonnementError.FinnesAllerede -> {
                            OpprettAbonnementMedEventError.AbonnementFinnesAllerede(it.abonnement)
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
