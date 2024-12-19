package no.nav.utenlandsadresser.app

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.fail
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Counter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.Adresselinje
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Land
import no.nav.utenlandsadresser.domain.Landkode
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.domain.Postnummer
import no.nav.utenlandsadresser.domain.Poststed
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import org.slf4j.Logger
import java.util.*

class FeedServiceTest :
    WordSpec({
        val feedRepository = mockk<FeedRepository>()
        val registeroppslagClient = mockk<RegisteroppslagClient>()
        val logger = mockk<Logger>(relaxed = true)
        val sporingsloggRepository = mockk<SporingsloggRepository>()
        val counter = mockk<Counter>(relaxed = true)
        val feedService = FeedService(feedRepository, registeroppslagClient, sporingsloggRepository, logger, counter)

        val identitetsnummer = Identitetsnummer("12345678901")
        val abonnementId = UUID.randomUUID()
        val feedEvent =
            FeedEvent.Outgoing(
                identitetsnummer = identitetsnummer,
                abonnementId = abonnementId,
                hendelsestype = Hendelsestype.OppdatertAdresse,
            )

        "readFeed" should {
            "return error when feed event is not found" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns null

                val result = feedService.readNext(Løpenummer(1), Organisasjonsnummer("123456789"))

                result shouldBe ReadFeedError.FeedEventNotFound.left()
            }

            "return error when failing to get postadresse" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { registeroppslagClient.getPostadresse(any()) } returns GetPostadresseError.UgyldigForespørsel.left()

                val result = feedService.readNext(Løpenummer(1), Organisasjonsnummer("123456789"))

                result shouldBe ReadFeedError.FailedToGetPostadresse.left()
            }

            "return null when postadresse is norsk" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { registeroppslagClient.getPostadresse(any()) } returns
                    Postadresse
                        .Norsk(
                            adresselinje1 = null,
                            adresselinje2 = null,
                            adresselinje3 = null,
                            postnummer = null,
                            poststed = null,
                            landkode = Landkode("NO"),
                            land = Land("Norge"),
                        ).right()

                val result =
                    feedService
                        .readNext(Løpenummer(1), Organisasjonsnummer("123456789"))
                        .getOrElse { fail("Expected postadresse") }

                result.first shouldBe feedEvent
                result.second.shouldBeNull()
            }

            "return postadresse when postadresse is utenlandsk" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { registeroppslagClient.getPostadresse(any()) } returns
                    Postadresse
                        .Utenlandsk(
                            adresselinje1 = Adresselinje("Adresselinje 1"),
                            adresselinje2 = Adresselinje("Adresselinje 2"),
                            adresselinje3 = Adresselinje("Adresselinje 3"),
                            postnummer = Postnummer("1234"),
                            poststed = Poststed("Poststed"),
                            landkode = Landkode("SE"),
                            land = Land("Sverige"),
                        ).right()
                coEvery { sporingsloggRepository.loggPostadresse(any(), any(), any(), any()) } returns Unit

                val result = feedService.readNext(Løpenummer(1), Organisasjonsnummer("123456789"))

                result.isRight() shouldBe true
                coVerify(exactly = 1) { sporingsloggRepository.loggPostadresse(any(), any(), any(), any()) }
                coVerify(exactly = 1) { counter.increment() }
            }

            "return event type adressebeskyttelse when hendelsestype is adressebeskyttelse" {
                val adressebeskyttelseEvent =
                    FeedEvent.Outgoing(
                        identitetsnummer = identitetsnummer,
                        abonnementId = abonnementId,
                        hendelsestype = Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT),
                    )
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns adressebeskyttelseEvent

                val result = feedService.readNext(Løpenummer(1), Organisasjonsnummer("123456789"))

                result.isRight() shouldBe true
                result.getOrElse { fail("Expected event") }.first shouldBe adressebeskyttelseEvent
                result.getOrElse { fail("Expected empty postadresse") }.second.shouldBeNull()
            }
        }
    })
