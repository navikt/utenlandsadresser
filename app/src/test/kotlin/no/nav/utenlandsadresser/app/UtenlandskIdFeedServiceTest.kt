package no.nav.utenlandsadresser.app

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Counter
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Iso3166Alpha3
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetKilde
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetsnummer
import no.nav.utenlandsadresser.infrastructure.client.HentUtenlandskId
import org.slf4j.Logger
import kotlin.uuid.Uuid

class UtenlandskIdFeedServiceTest :
    WordSpec({
        val feedRepository = mockk<UtenlandskIdFeedRepository>()
        val abonnementRepository = mockk<UtenlandskIdAbonnementRepository>()
        val hentUtenlandskId = mockk<HentUtenlandskId>()
        val sporingsloggRepository = mockk<SporingsloggRepository>(relaxed = true)
        val counter = mockk<Counter>(relaxed = true)
        val stoppetAbonnementCounter = mockk<Counter>(relaxed = true)
        val service =
            UtenlandskIdFeedService(
                feedRepository,
                abonnementRepository,
                hentUtenlandskId,
                sporingsloggRepository,
                mockk<Logger>(relaxed = true),
                counter,
                stoppetAbonnementCounter,
            )

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
            coEvery { abonnementRepository.finnesAbonnement(any(), any()) } returns true
        }

        "readNext" should {
            "return the event with an empty list without looking up PDL when the abonnement is stopped" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { abonnementRepository.finnesAbonnement(feedEvent.abonnementId, organisasjonsnummer) } returns false

                service.readNext(Løpenummer(0), organisasjonsnummer) shouldBe (feedEvent to emptyList<UtenlandskIdentitet>()).right()

                coVerify(exactly = 0) { hentUtenlandskId.hentUtenlandskIdentitet(any()) }
                coVerify(exactly = 0) { sporingsloggRepository.loggUtenlandskId(any(), any(), any(), any()) }
                verify(exactly = 0) { counter.increment() }
                verify(exactly = 1) { stoppetAbonnementCounter.increment() }
            }

            "check the abonnement against the organisasjonsnummer of the caller" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(any()) } returns emptyList<UtenlandskIdentitet>().right()

                service.readNext(Løpenummer(0), organisasjonsnummer)

                coVerify(exactly = 1) { abonnementRepository.finnesAbonnement(feedEvent.abonnementId, organisasjonsnummer) }
            }
            "read the event after the given løpenummer" {
                coEvery { feedRepository.getFeedEvent(organisasjonsnummer, Løpenummer(6)) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(feedEvent.identitetsnummer) } returns
                    listOf(utenlandskIdentitet).right()

                service.readNext(Løpenummer(5), organisasjonsnummer) shouldBe (feedEvent to listOf(utenlandskIdentitet)).right()
            }

            "log the delivery in sporingslogg and count it when utenlandsk id is delivered" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(any()) } returns listOf(utenlandskIdentitet).right()

                service.readNext(Løpenummer(0), organisasjonsnummer)

                coVerify(exactly = 1) {
                    sporingsloggRepository.loggUtenlandskId(
                        feedEvent.identitetsnummer,
                        organisasjonsnummer,
                        listOf(utenlandskIdentitet),
                        any(),
                    )
                }
                verify(exactly = 1) { counter.increment() }
            }

            "return the event with an empty list when the person no longer has utenlandsk id" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(any()) } returns emptyList<UtenlandskIdentitet>().right()

                service.readNext(Løpenummer(0), organisasjonsnummer) shouldBe (feedEvent to emptyList<UtenlandskIdentitet>()).right()

                coVerify(exactly = 0) { sporingsloggRepository.loggUtenlandskId(any(), any(), any(), any()) }
                verify(exactly = 0) { counter.increment() }
            }

            "return not found when there is no event on the next løpenummer" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns null

                service.readNext(Løpenummer(0), organisasjonsnummer) shouldBe ReadUtenlandskIdFeedError.FeedEventNotFound.left()
            }

            "return an error and log nothing to sporingslogg when the lookup fails" {
                coEvery { feedRepository.getFeedEvent(any(), any()) } returns feedEvent
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(any()) } returns HentUtenlandskId.Error.FeilIRespons.left()

                service.readNext(Løpenummer(0), organisasjonsnummer) shouldBe
                    ReadUtenlandskIdFeedError.FailedToGetUtenlandskId.left()

                coVerify(exactly = 0) { sporingsloggRepository.loggUtenlandskId(any(), any(), any(), any()) }
            }
        }
    })
