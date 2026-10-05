package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Isolate
class FeedPostgresRepositoryTest :
    WordSpec({
        val database = setupDatabase()

        val feedRepository = PostgresFeedRepository(database, Clock.System)

        val skatteetaten = Organisasjonsnummer("974761076")
        val annenMottaker = Organisasjonsnummer("889640782")

        fun incoming(
            organisasjonsnummer: Organisasjonsnummer,
            identitetsnummer: Identitetsnummer = Identitetsnummer("12345678910"),
        ) = FeedEvent.Incoming(
            identitetsnummer = identitetsnummer,
            abonnementId = Uuid.random(),
            hendelsestype = Hendelsestype.OppdatertAdresse,
            organisasjonsnummer = organisasjonsnummer,
        )

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

            "count løpenummer separately per organisasjonsnummer" {
                val første = incoming(skatteetaten)
                val andre = incoming(skatteetaten)
                val annenEvent = incoming(annenMottaker)
                feedRepository.createFeedEvent(første)
                feedRepository.createFeedEvent(andre)
                feedRepository.createFeedEvent(annenEvent)

                feedRepository.getFeedEvent(skatteetaten, Løpenummer(1))?.abonnementId shouldBe første.abonnementId
                feedRepository.getFeedEvent(skatteetaten, Løpenummer(2))?.abonnementId shouldBe andre.abonnementId
                feedRepository.getFeedEvent(skatteetaten, Løpenummer(3)).shouldBeNull()
                feedRepository.getFeedEvent(annenMottaker, Løpenummer(1))?.abonnementId shouldBe annenEvent.abonnementId
            }

            "give unique and gapless løpenummer when events for different persons are created concurrently" {
                val antall = 25
                val identer = (1..antall).map { Identitetsnummer("1234567%04d".format(it)) }

                coroutineScope {
                    identer
                        .map { ident -> async(Dispatchers.IO) { feedRepository.createFeedEvent(incoming(skatteetaten, ident)) } }
                        .awaitAll()
                }

                val lagredeIdenter =
                    (1..antall).map { løpenummer ->
                        feedRepository.getFeedEvent(skatteetaten, Løpenummer(løpenummer))?.identitetsnummer
                    }
                lagredeIdenter shouldContainExactlyInAnyOrder identer
                feedRepository.getFeedEvent(skatteetaten, Løpenummer(antall + 1)).shouldBeNull()
            }

            "not share løpenummer with the utenlandsk id feed" {
                PostgresUtenlandskIdFeedRepository(database, Clock.System).createFeedEvent(
                    UtenlandskIdFeedEvent.Incoming(
                        identitetsnummer = Identitetsnummer("12345678910"),
                        abonnementId = Uuid.random(),
                        hendelsestype = UtenlandskIdHendelsestype.OppdatertUtenlandskId,
                        organisasjonsnummer = skatteetaten,
                    ),
                )
                val event = incoming(skatteetaten)

                feedRepository.createFeedEvent(event)

                feedRepository.getFeedEvent(skatteetaten, Løpenummer(1))?.abonnementId shouldBe event.abonnementId
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
