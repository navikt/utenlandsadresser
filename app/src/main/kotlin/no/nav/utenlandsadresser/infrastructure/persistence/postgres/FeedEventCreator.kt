package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.coroutines.Dispatchers
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.infrastructure.kafka.Livshendelse
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.seconds

class FeedEventCreator(
    private val feedRepository: FeedPostgresRepository,
    private val abonnementRepository: AbonnementPostgresRepository,
) {
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
                            if (hasEventBeenAddedInTheLast(10.seconds, it.identitetsnummer, it.abonnementId)) {
                                return@forEach
                            }
                            createFeedEvent(it)
                        }
                }
            }
        }
    }
}
