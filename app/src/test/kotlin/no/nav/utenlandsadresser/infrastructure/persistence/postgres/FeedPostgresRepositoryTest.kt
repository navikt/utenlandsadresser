package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.utenlandsadresser.FastClock
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Isolate
class FeedPostgresRepositoryTest :
    WordSpec({
        val database = setupDatabase()

        val fastClock = FastClock()
        val feedRepository = PostgresFeedRepository(database, fastClock)

        beforeTest { fastClock.nå = Instant.parse("2025-01-01T12:00:00Z") }

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
                PostgresUtenlandskIdFeedRepository(database, fastClock).createFeedEvent(
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
            val feedEvent =
                FeedEvent.Incoming(
                    identitetsnummer = Identitetsnummer("12345678910"),
                    abonnementId = Uuid.random(),
                    hendelsestype = Hendelsestype.OppdatertAdresse,
                    organisasjonsnummer = Organisasjonsnummer("889640782"),
                )

            suspend fun hasEventBeenAddedInTheLast10Seconds() =
                feedRepository.hasEventBeenAddedInTheLast(
                    10.seconds,
                    feedEvent.identitetsnummer,
                    feedEvent.abonnementId,
                    feedEvent.hendelsestype,
                )

            "return true when the event was added exactly 10 seconds ago" {
                feedRepository.createFeedEvent(feedEvent)
                fastClock.nå += 10.seconds

                hasEventBeenAddedInTheLast10Seconds() shouldBe true
            }

            "return false when the event was added more than 10 seconds ago" {
                feedRepository.createFeedEvent(feedEvent)
                fastClock.nå += 10.seconds + 1.milliseconds

                hasEventBeenAddedInTheLast10Seconds() shouldBe false
            }

            "return false for another hendelsestype" {
                feedRepository.createFeedEvent(feedEvent)

                feedRepository.hasEventBeenAddedInTheLast(
                    10.seconds,
                    feedEvent.identitetsnummer,
                    feedEvent.abonnementId,
                    Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT),
                ) shouldBe false
            }
        }
    })
