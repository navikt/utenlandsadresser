package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementMedEventError
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementOppretter
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class PostgresUtenlandskIdAbonnementOppretter(
    private val abonnementRepository: PostgresUtenlandskIdAbonnementRepository,
    private val feedRepository: PostgresUtenlandskIdFeedRepository,
    private val database: R2dbcDatabase,
) : UtenlandskIdAbonnementOppretter {
    /**
     * Oppretter abonnementet hvis det ikke finnes fra før. Finnes det, returneres det eksisterende abonnementet.
     *
     * Uansett om abonnementet finnes eller ikke, legges det en hendelse på feeden når personen har utenlandsk id.
     * Da får mottakeren alltid gjeldende id-er når abonnementet startes. Abonnementet og hendelsen lagres i
     * samme transaksjon.
     */
    override suspend fun opprettMedEvent(
        abonnement: Abonnement,
        harUtenlandskId: Boolean,
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
