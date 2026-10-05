package no.nav.utenlandsadresser.application.service

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.utenlandsadresser.FastClock
import no.nav.utenlandsadresser.application.port.outbound.Feed
import no.nav.utenlandsadresser.application.port.outbound.HentUtenlandskId
import no.nav.utenlandsadresser.application.port.outbound.SporingsloggRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdFeedRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Iso3166Alpha3
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetKilde
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetsnummer
import kotlin.uuid.Uuid

class UtenlandskIdFeedServiceTest :
    WordSpec({
        val feedRepository = mockk<UtenlandskIdFeedRepository>()
        val abonnementRepository = mockk<UtenlandskIdAbonnementRepository>()
        val hentUtenlandskId = mockk<HentUtenlandskId>()
        val sporingsloggRepository = mockk<SporingsloggRepository>(relaxed = true)
        val metrikker = TestMetrikker()
        val fastClock = FastClock()
        val service =
            UtenlandskIdFeedService(
                feedRepository,
                abonnementRepository,
                hentUtenlandskId,
                sporingsloggRepository,
                fastClock,
            )

        suspend fun readNext(
            løpenummer: Løpenummer,
            organisasjonsnummer: Organisasjonsnummer,
        ) = context(metrikker) { service.readNext(løpenummer, organisasjonsnummer) }

        val organisasjonsnummer = Organisasjonsnummer("974761076")
        val feedEvent =
            UtenlandskIdFeedEvent.Outgoing(
                identitetsnummer = Identitetsnummer("12345678910"),
                abonnementId = Uuid.random(),
                hendelsestype = UtenlandskIdHendelsestype.OppdatertUtenlandskId,
            )
        val utenlandskIdentitet =
            UtenlandskIdentitet(
                identitetsnummer = UtenlandskIdentitetsnummer("123010190B456"),
                utstederland = Iso3166Alpha3("DEU"),
                kilde = UtenlandskIdentitetKilde("Dolly"),
            )

        beforeTest {
            clearAllMocks(answers = false)
            metrikker.nullstill()
            coEvery { abonnementRepository.finnesAbonnement(any(), any()) } returns true
        }

        "readNext" should {
            "return the event with an empty list without looking up PDL when the abonnement is stopped" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { abonnementRepository.finnesAbonnement(feedEvent.abonnementId, organisasjonsnummer) } returns false

                readNext(Løpenummer(0), organisasjonsnummer) shouldBe (feedEvent to emptyList<UtenlandskIdentitet>()).right()

                coVerify(exactly = 0) { hentUtenlandskId.hentUtenlandskIdentitet(any()) }
                coVerify(exactly = 0) { sporingsloggRepository.loggUtenlandskId(any(), any(), any(), any()) }
                metrikker.utlevert[Feed.UTENLANDSK_ID] shouldBe null
                metrikker.stoppetAbonnementLest[Feed.UTENLANDSK_ID] shouldBe 1
            }

            "check the abonnement against the organisasjonsnummer of the caller" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(any()) } returns emptyList<UtenlandskIdentitet>().right()

                readNext(Løpenummer(0), organisasjonsnummer)

                coVerify(exactly = 1) { abonnementRepository.finnesAbonnement(feedEvent.abonnementId, organisasjonsnummer) }
            }
            "read the event after the given løpenummer" {
                coEvery { feedRepository.getFeedEvent(organisasjonsnummer, Løpenummer(6)) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(feedEvent.identitetsnummer) } returns
                    listOf(utenlandskIdentitet).right()

                readNext(Løpenummer(5), organisasjonsnummer) shouldBe (feedEvent to listOf(utenlandskIdentitet)).right()
            }

            "log the delivery in sporingslogg and count it when utenlandsk id is delivered" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(any()) } returns listOf(utenlandskIdentitet).right()

                readNext(Løpenummer(0), organisasjonsnummer)

                coVerify(exactly = 1) {
                    sporingsloggRepository.loggUtenlandskId(
                        feedEvent.identitetsnummer,
                        organisasjonsnummer,
                        listOf(utenlandskIdentitet),
                        fastClock.nå,
                    )
                }
                metrikker.utlevert[Feed.UTENLANDSK_ID] shouldBe 1
            }

            "return the event with an empty list when the person no longer has utenlandsk id" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(any()) } returns emptyList<UtenlandskIdentitet>().right()

                readNext(Løpenummer(0), organisasjonsnummer) shouldBe (feedEvent to emptyList<UtenlandskIdentitet>()).right()

                coVerify(exactly = 0) { sporingsloggRepository.loggUtenlandskId(any(), any(), any(), any()) }
                metrikker.utlevert[Feed.UTENLANDSK_ID] shouldBe null
            }

            "return not found when there is no event on the next løpenummer" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns null

                readNext(Løpenummer(0), organisasjonsnummer) shouldBe ReadUtenlandskIdFeedError.FeedEventNotFound.left()
            }

            "return an error and log nothing to sporingslogg when the lookup fails" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(any()) } returns HentUtenlandskId.Error.FeilIRespons.left()

                readNext(Løpenummer(0), organisasjonsnummer) shouldBe
                    ReadUtenlandskIdFeedError.FailedToGetUtenlandskId.left()

                coVerify(exactly = 0) { sporingsloggRepository.loggUtenlandskId(any(), any(), any(), any()) }
            }
        }
    })
