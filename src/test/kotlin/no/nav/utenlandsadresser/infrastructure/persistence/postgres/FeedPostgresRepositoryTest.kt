package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import kotest.extension.setupDatabase
import kotlinx.coroutines.Dispatchers
import no.nav.utenlandsadresser.domain.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@DoNotParallelize
class FeedpostgresRepositoryTest : WordSpec({
    val database = setupDatabase()

    val feedRepository = FeedPostgresRepository(database)

    "create feed event" should {
        "insert a new feed event" {
            val feedEvent = FeedEvent.Incoming(
                identitetsnummer = Identitetsnummer("12345678910"),
                abonnementId = UUID.randomUUID(),
                hendelsestype = Hendelsestype.OppdatertAdresse,
                organisasjonsnummer = Organisasjonsnummer("889640782")
            )

            with(feedRepository) {
                newSuspendedTransaction(Dispatchers.IO, database) {
                    createFeedEvent(feedEvent)
                }
            }

            feedRepository.getFeedEvent(feedEvent.organisasjonsnummer, Løpenummer(1)) shouldBe FeedEvent.Outgoing(
                identitetsnummer = feedEvent.identitetsnummer,
                abonnementId = feedEvent.abonnementId,
                hendelsestype = feedEvent.hendelsestype
            )
        }
    }
})
