package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.coroutines.Dispatchers
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.infrastructure.client.kafka.Livshendelse
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.Logger

class FeedEventCreator(
    private val feedRepository: FeedPostgresRepository,
    private val abonnementRepository: AbonnementPostgresRepository,
    private val logger: Logger
) {
    suspend fun createFeedEvent(livshendelse: Livshendelse) {
        with(abonnementRepository) {
            with(feedRepository) {
                newSuspendedTransaction(Dispatchers.IO) {
                    val abonnementer = getAbonnementer(livshendelse.personidenter)
                    logger.info("Abonnementer funnet: $abonnementer")
                    when (livshendelse) {
                        is Livshendelse.Adressebeskyttelse -> {
                            // TODO: Implementer håndtering av adressebeskyttelse
                            logger.info("Håndtering av adressebeskyttelse er ikke implementert")
                        }
                        is Livshendelse.Bostedsadresse,
                        is Livshendelse.Kontaktadresse -> abonnementer.forEach {
                            createFeedEvent(FeedEvent.Incoming(it.identitetsnummer, it.organisasjonsnummer))
                        }
                    }
                }
            }
        }
    }
}