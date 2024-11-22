package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.coroutines.Dispatchers
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.infrastructure.kafka.Livshendelse
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.seconds

class PostgresFeedEventCreator(
    private val feedRepository: PostgresFeedRepository,
    private val abonnementRepository: PostgresAbonnementRepository,
) {
    /**
     * Oppretter et feed event i databasen hvis det finnes et aktivt abonnement for personen.
     */
    suspend fun createFeedEvent(livshendelse: Livshendelse) {
        with(abonnementRepository) {
            with(feedRepository) {
                newSuspendedTransaction(Dispatchers.IO) {
                    val abonnementer = getAbonnementer(livshendelse.personidenter)

                    abonnementer
                        .map {
                            when (livshendelse) {
                                is Livshendelse.Adressebeskyttelse ->
                                    FeedEvent.Incoming(
                                        identitetsnummer = it.identitetsnummer,
                                        abonnementId = it.id,
                                        hendelsestype = livshendelse.adressebeskyttelse.toDomain(),
                                        organisasjonsnummer = it.organisasjonsnummer,
                                    )

                                is Livshendelse.Bostedsadresse,
                                is Livshendelse.Kontaktadresse,
                                ->
                                    FeedEvent.Incoming(
                                        identitetsnummer = it.identitetsnummer,
                                        abonnementId = it.id,
                                        hendelsestype = Hendelsestype.OppdatertAdresse,
                                        organisasjonsnummer = it.organisasjonsnummer,
                                    )
                            }
                        }.forEach {
                            /*
                            Når vi mottar meldinger om at det har skjedd en endring på en adresse, så får vi
                            en melding for hvert felt som er endret. Dette skaper mye unødvendig støy i feeden.
                            Derfor prøver vi å unngå å lage "duplikater" i feeden ved å sjekke om det allerede
                            er en lik event i feeden innenfor en gitt periode.
                             */
                            if (hasEventBeenAddedInTheLast(
                                    10.seconds,
                                    it.identitetsnummer,
                                    it.abonnementId,
                                    it.hendelsestype,
                                )
                            ) {
                                return@forEach
                            }
                            createFeedEvent(it)
                        }
                }
            }
        }
    }
}
