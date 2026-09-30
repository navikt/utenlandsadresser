package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.utenlandsadresser.app.UtenlandskIdAbonnementInitializer
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import no.nav.utenlandsadresser.infrastructure.persistence.CreateAbonnementError
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class PostgresUtenlandskIdAbonnementInitializer(
    private val abonnementRepository: PostgresUtenlandskIdAbonnementRepository,
    private val feedRepository: PostgresUtenlandskIdFeedRepository,
    private val database: R2dbcDatabase,
) : UtenlandskIdAbonnementInitializer {
    /**
     * Oppretter abonnementet hvis det ikke finnes fra før. Finnes det, returneres det eksisterende abonnementet.
     *
     * Uansett om abonnementet finnes eller ikke, legges det en hendelse på feeden når personen har utenlandsk id.
     * Da får mottakeren alltid gjeldende id-er når abonnementet startes. Abonnementet og hendelsen lagres i
     * samme transaksjon.
     */
    override suspend fun initAbonnement(
        abonnement: Abonnement,
        harUtenlandskId: Boolean,
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

            if (harUtenlandskId) {
                feedRepository.createFeedEvent(
                    UtenlandskIdFeedEvent.Incoming(
                        identitetsnummer = abonnementFromRepository.identitetsnummer,
                        abonnementId = abonnementFromRepository.id,
                        hendelsestype = UtenlandskIdHendelsestype.OppdatertUtenlandskId,
                        organisasjonsnummer = abonnementFromRepository.organisasjonsnummer,
                    ),
                )
            }

            createAbonnementResult
        }
}
