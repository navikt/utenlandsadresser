package no.nav.utenlandsadresser.application.service

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.fail
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.utenlandsadresser.FastClock
import no.nav.utenlandsadresser.application.port.inbound.LesFeedError
import no.nav.utenlandsadresser.application.port.outbound.AbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.Feed
import no.nav.utenlandsadresser.application.port.outbound.FeedRepository
import no.nav.utenlandsadresser.application.port.outbound.HentPostadresseError
import no.nav.utenlandsadresser.application.port.outbound.PostadresseOppslag
import no.nav.utenlandsadresser.application.port.outbound.SporingsloggRepository
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
import kotlin.uuid.Uuid

class FeedServiceTest :
    WordSpec({
        val feedRepository = mockk<FeedRepository>()
        val abonnementRepository = mockk<AbonnementRepository>()
        val postadresseOppslag = mockk<PostadresseOppslag>()
        val sporingsloggRepository = mockk<SporingsloggRepository>()
        val metrikker = TestMetrikker()
        val fastClock = FastClock()
        val feedService =
            FeedService(
                feedRepository,
                abonnementRepository,
                postadresseOppslag,
                sporingsloggRepository,
                fastClock,
            )

        suspend fun lesNeste(
            løpenummer: Løpenummer,
            organisasjonsnummer: Organisasjonsnummer,
        ) = context(metrikker) { feedService.lesNeste(løpenummer, organisasjonsnummer) }

        val identitetsnummer = Identitetsnummer("12345678901")
        val abonnementId = Uuid.random()
        val feedEvent =
            FeedEvent.Outgoing(
                identitetsnummer = identitetsnummer,
                abonnementId = abonnementId,
                hendelsestype = Hendelsestype.OppdatertAdresse,
            )

        beforeTest {
            clearAllMocks(answers = false)
            metrikker.nullstill()
            coEvery { abonnementRepository.finnesAbonnement(any(), any()) } returns true
        }

        "readFeed" should {
            "return the event without postadresse when the abonnement is stopped" {
                val organisasjonsnummer = Organisasjonsnummer("123456789")
                coEvery { feedRepository.hentFeedEvent(any(), any()) } returns feedEvent
                coEvery { abonnementRepository.finnesAbonnement(abonnementId, organisasjonsnummer) } returns false

                val result = lesNeste(Løpenummer(1), organisasjonsnummer)

                result shouldBe (feedEvent to null).right()
                coVerify(exactly = 0) { postadresseOppslag.hentPostadresse(any()) }
                coVerify(exactly = 0) { sporingsloggRepository.loggPostadresse(any(), any(), any(), any()) }
                metrikker.utlevert[Feed.POSTADRESSE] shouldBe null
                metrikker.stoppetAbonnementLest[Feed.POSTADRESSE] shouldBe 1
            }

            "check the abonnement against the organisasjonsnummer of the caller" {
                val organisasjonsnummer = Organisasjonsnummer("123456789")
                coEvery { feedRepository.hentFeedEvent(any(), any()) } returns feedEvent
                coEvery { postadresseOppslag.hentPostadresse(any()) } returns HentPostadresseError.UkjentAdresse.left()

                lesNeste(Løpenummer(1), organisasjonsnummer)

                coVerify(exactly = 1) { abonnementRepository.finnesAbonnement(abonnementId, organisasjonsnummer) }
            }

            "deliver adressebeskyttelse even when the abonnement is stopped" {
                val adressebeskyttelseEvent =
                    FeedEvent.Outgoing(
                        identitetsnummer = identitetsnummer,
                        abonnementId = abonnementId,
                        hendelsestype = Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT),
                    )
                coEvery { feedRepository.hentFeedEvent(any(), any()) } returns adressebeskyttelseEvent
                coEvery { abonnementRepository.finnesAbonnement(any(), any()) } returns false

                val result = lesNeste(Løpenummer(1), Organisasjonsnummer("123456789"))

                result shouldBe (adressebeskyttelseEvent to null).right()
                metrikker.stoppetAbonnementLest[Feed.POSTADRESSE] shouldBe null
            }
            "return error when feed event is not found" {
                coEvery { feedRepository.hentFeedEvent(any(), any()) } returns null

                val result = lesNeste(Løpenummer(1), Organisasjonsnummer("123456789"))

                result shouldBe LesFeedError.FeedEventIkkeFunnet.left()
            }

            "return error when failing to get postadresse" {
                coEvery { feedRepository.hentFeedEvent(any(), any()) } returns feedEvent
                coEvery { postadresseOppslag.hentPostadresse(any()) } returns HentPostadresseError.UgyldigForespørsel.left()

                val result = lesNeste(Løpenummer(1), Organisasjonsnummer("123456789"))

                result shouldBe LesFeedError.KunneIkkeHentePostadresse.left()
            }

            "return null when postadresse is norsk" {
                coEvery { feedRepository.hentFeedEvent(any(), any()) } returns feedEvent
                coEvery { postadresseOppslag.hentPostadresse(any()) } returns
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
                    lesNeste(Løpenummer(1), Organisasjonsnummer("123456789"))
                        .getOrElse { fail("Expected postadresse") }

                result.first shouldBe feedEvent
                result.second.shouldBeNull()
            }

            "return postadresse when postadresse is utenlandsk" {
                coEvery { feedRepository.hentFeedEvent(any(), any()) } returns feedEvent
                coEvery { postadresseOppslag.hentPostadresse(any()) } returns
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

                val result = lesNeste(Løpenummer(1), Organisasjonsnummer("123456789"))

                result.isRight() shouldBe true
                coVerify(exactly = 1) { sporingsloggRepository.loggPostadresse(any(), any(), any(), fastClock.nå) }
                metrikker.utlevert[Feed.POSTADRESSE] shouldBe 1
            }

            "return event type adressebeskyttelse when hendelsestype is adressebeskyttelse" {
                val adressebeskyttelseEvent =
                    FeedEvent.Outgoing(
                        identitetsnummer = identitetsnummer,
                        abonnementId = abonnementId,
                        hendelsestype = Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT),
                    )
                coEvery { feedRepository.hentFeedEvent(any(), any()) } returns adressebeskyttelseEvent

                val result = lesNeste(Løpenummer(1), Organisasjonsnummer("123456789"))

                result.isRight() shouldBe true
                result.getOrElse { fail("Expected event") }.first shouldBe adressebeskyttelseEvent
                result.getOrElse { fail("Expected empty postadresse") }.second.shouldBeNull()
            }
        }
    })
