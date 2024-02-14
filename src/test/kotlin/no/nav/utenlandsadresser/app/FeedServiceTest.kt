package no.nav.utenlandsadresser.app

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.fail
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.persistence.FeedRepository

class FeedServiceTest : WordSpec({
    val feedRepository = mockk<FeedRepository>()
    val registeroppslagClient = mockk<RegisteroppslagClient>()
    val feedService = FeedService(feedRepository, registeroppslagClient)

    val feedEvent = FeedEvent.Outgoing(
        Identitetsnummer("12345678901").getOrElse { fail("Invalid identitetsnummer") },
        Organisasjonsnummer("123456789"),
        Løpenummer(1),
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

        "return error when postadresse is norsk" {
            coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
            coEvery { registeroppslagClient.getPostadresse(any()) } returns Postadresse.Norsk(
                adresselinje1 = null,
                adresselinje2 = null,
                adresselinje3 = null,
                postnummer = null,
                poststed = null,
                landkode = Landkode("NO"),
                land = Land("Norge"),
            ).right()

            val result = feedService.readNext(Løpenummer(1), Organisasjonsnummer("123456789"))

            result shouldBe ReadFeedError.PostadresseNotFound.left()
        }

        "return error when postadresse is empty" {
            coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
            coEvery { registeroppslagClient.getPostadresse(any()) } returns Postadresse.Empty.right()

            val result = feedService.readNext(Løpenummer(1), Organisasjonsnummer("123456789"))

            result shouldBe ReadFeedError.PostadresseNotFound.left()
        }

        "return postadresse when postadresse is utenlandsk" {
            coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
            coEvery { registeroppslagClient.getPostadresse(any()) } returns Postadresse.Utenlandsk(
                adresselinje1 = Adresselinje("Adresselinje 1"),
                adresselinje2 = Adresselinje("Adresselinje 2"),
                adresselinje3 = Adresselinje("Adresselinje 3"),
                postnummer = Postnummer("1234"),
                poststed = Poststed("Poststed"),
                landkode = Landkode("SE"),
                land = Land("Sverige"),
            ).right()

            val result = feedService.readNext(Løpenummer(1), Organisasjonsnummer("123456789"))

            result.isRight() shouldBe true
        }
    }
})
