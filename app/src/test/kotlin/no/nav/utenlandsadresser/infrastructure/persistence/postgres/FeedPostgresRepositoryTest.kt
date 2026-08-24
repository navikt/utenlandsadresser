package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Isolate
class FeedPostgresRepositoryTest :
    WordSpec({
        val database = setupDatabase()

        val feedRepository = PostgresFeedRepository(database)

        "create feed event" should {
            "insert a new feed event" {
                val feedEvent =
                    FeedEvent.Incoming(
                        identitetsnummer = Identitetsnummer("12345678910"),
                        abonnementId = Uuid.random(),
                        hendelsestype = Hendelsestype.OppdatertAdresse,
                        organisasjonsnummer = Organisasjonsnummer("889640782"),
                    )

                feedRepository.createFeedEvent(feedEvent)

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
                        abonnementId = Uuid.random(),
                        hendelsestype = Hendelsestype.OppdatertAdresse,
                        organisasjonsnummer = Organisasjonsnummer("889640782"),
                    )

                feedRepository.createFeedEvent(feedEvent)

                val result =
                    feedRepository.hasEventBeenAddedInTheLast(
                        10.seconds,
                        feedEvent.identitetsnummer,
                        feedEvent.abonnementId,
                        feedEvent.hendelsestype,
                    )

                result shouldBe true
            }

            "return false if event has not been added the last 10 seconds" {
                val feedEvent =
                    FeedEvent.Incoming(
                        identitetsnummer = Identitetsnummer("12345678910"),
                        abonnementId = Uuid.random(),
                        hendelsestype = Hendelsestype.OppdatertAdresse,
                        organisasjonsnummer = Organisasjonsnummer("889640782"),
                    )

                feedRepository.createFeedEvent(
                    feedEvent,
                    Clock.System
                        .now()
                        .minus(20.seconds),
                )

                val result =
                    feedRepository.hasEventBeenAddedInTheLast(
                        10.seconds,
                        feedEvent.identitetsnummer,
                        feedEvent.abonnementId,
                        feedEvent.hendelsestype,
                    )

                result shouldBe false
            }
        }
    })
