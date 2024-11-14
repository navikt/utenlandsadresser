package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import kotlin.time.Duration.Companion.seconds

@DoNotParallelize
class FeedpostgresRepositoryTest :
    WordSpec({
        val database = setupDatabase()

        val feedRepository = PostgresFeedRepository(database)

        "create feed event" should {
            "insert a new feed event" {
                val feedEvent =
                    FeedEvent.Incoming(
                        identitetsnummer = Identitetsnummer("12345678910"),
                        abonnementId = UUID.randomUUID(),
                        hendelsestype = Hendelsestype.OppdatertAdresse,
                        organisasjonsnummer = Organisasjonsnummer("889640782"),
                    )

                with(feedRepository) {
                    newSuspendedTransaction(Dispatchers.IO, database) {
                        createFeedEvent(feedEvent)
                    }
                }

                feedRepository.getFeedEvent(feedEvent.organisasjonsnummer, Løpenummer(1)) shouldBe
                    FeedEvent.Outgoing(
                        identitetsnummer = feedEvent.identitetsnummer,
                        abonnementId = feedEvent.abonnementId,
                        hendelsestype = feedEvent.hendelsestype,
                    )
            }
        }

        "has event been added the last" should {
            "return true if event has been added the last 10 seconds" {
                val feedEvent =
                    FeedEvent.Incoming(
                        identitetsnummer = Identitetsnummer("12345678910"),
                        abonnementId = UUID.randomUUID(),
                        hendelsestype = Hendelsestype.OppdatertAdresse,
                        organisasjonsnummer = Organisasjonsnummer("889640782"),
                    )

                with(feedRepository) {
                    newSuspendedTransaction(Dispatchers.IO, database) {
                        createFeedEvent(feedEvent)
                    }
                }

                val result =
                    with(feedRepository) {
                        newSuspendedTransaction(Dispatchers.IO, database) {
                            hasEventBeenAddedInTheLast(
                                10.seconds,
                                feedEvent.identitetsnummer,
                                feedEvent.abonnementId,
                                feedEvent.hendelsestype,
                            )
                        }
                    }

                result shouldBe true
            }

            "return false if event has not been added the last 10 seconds" {
                val feedEvent =
                    FeedEvent.Incoming(
                        identitetsnummer = Identitetsnummer("12345678910"),
                        abonnementId = UUID.randomUUID(),
                        hendelsestype = Hendelsestype.OppdatertAdresse,
                        organisasjonsnummer = Organisasjonsnummer("889640782"),
                    )

                with(feedRepository) {
                    newSuspendedTransaction(Dispatchers.IO, database) {
                        createFeedEvent(feedEvent, Clock.System.now().minus(20.seconds))
                    }
                }

                val result =
                    with(feedRepository) {
                        newSuspendedTransaction(Dispatchers.IO, database) {
                            hasEventBeenAddedInTheLast(
                                10.seconds,
                                feedEvent.identitetsnummer,
                                feedEvent.abonnementId,
                                feedEvent.hendelsestype,
                            )
                        }
                    }

                result shouldBe false
            }
        }
    })
