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
import no.nav.utenlandsadresser.application.port.outbound.DeleteAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.HentUtenlandskId
import no.nav.utenlandsadresser.application.port.outbound.InitAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementInitializer
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Iso3166Alpha3
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetKilde
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetsnummer
import kotlin.time.Clock
import kotlin.uuid.Uuid

class UtenlandskIdAbonnementServiceTest :
    WordSpec({
        val abonnementRepository = mockk<UtenlandskIdAbonnementRepository>()
        val hentUtenlandskId = mockk<HentUtenlandskId>()
        val initializer = mockk<UtenlandskIdAbonnementInitializer>()
        val service = UtenlandskIdAbonnementService(abonnementRepository, hentUtenlandskId, initializer, FastClock())

        val identitetsnummer = Identitetsnummer("12345678910")
        val organisasjonsnummer = Organisasjonsnummer("974761076")
        val utenlandskIdentitet =
            UtenlandskIdentitet(
                identitetsnummer = UtenlandskIdentitetsnummer("123010190B456"),
                utstederland = Iso3166Alpha3("DEU"),
                kilde = UtenlandskIdentitetKilde("Dolly"),
            )
        val eksisterendeAbonnement =
            Abonnement(Uuid.random(), organisasjonsnummer, identitetsnummer, Clock.System.now())

        beforeTest { clearAllMocks() }

        "startAbonnement" should {
            "create a feed event when the person has utenlandsk id" {
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(identitetsnummer) } returns listOf(utenlandskIdentitet).right()
                coEvery { initializer.initAbonnement(any(), any()) } answers { firstArg<Abonnement>().right() }

                val result = service.startAbonnement(identitetsnummer, organisasjonsnummer)

                result.isRight() shouldBe true
                coVerify(exactly = 1) {
                    initializer.initAbonnement(
                        match { it.identitetsnummer == identitetsnummer && it.organisasjonsnummer == organisasjonsnummer },
                        harUtenlandskId = true,
                    )
                }
            }

            "not create a feed event when the person has no utenlandsk id" {
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(identitetsnummer) } returns emptyList<UtenlandskIdentitet>().right()
                coEvery { initializer.initAbonnement(any(), any()) } answers { firstArg<Abonnement>().right() }

                service.startAbonnement(identitetsnummer, organisasjonsnummer).isRight() shouldBe true

                coVerify(exactly = 1) { initializer.initAbonnement(any(), harUtenlandskId = false) }
            }

            "not create the abonnement when the lookup fails" {
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(identitetsnummer) } returns
                    HentUtenlandskId.Error.Kommunikasjonsfeil.left()

                service.startAbonnement(identitetsnummer, organisasjonsnummer) shouldBe
                    StartUtenlandskIdAbonnementError.FailedToGetUtenlandskId.left()

                coVerify(exactly = 0) { initializer.initAbonnement(any(), any()) }
            }

            "return the existing abonnement when it already exists" {
                coEvery { hentUtenlandskId.hentUtenlandskIdentitet(identitetsnummer) } returns listOf(utenlandskIdentitet).right()
                coEvery { initializer.initAbonnement(any(), any()) } returns
                    InitAbonnementError.AbonnementAlreadyExists(eksisterendeAbonnement).left()

                service.startAbonnement(identitetsnummer, organisasjonsnummer) shouldBe
                    StartUtenlandskIdAbonnementError.AbonnementAlreadyExists(eksisterendeAbonnement).left()
            }
        }

        "stopAbonnement" should {
            "stop the abonnement" {
                coEvery { abonnementRepository.deleteAbonnement(eksisterendeAbonnement.id, organisasjonsnummer) } returns Unit.right()

                service.stopAbonnement(eksisterendeAbonnement.id, organisasjonsnummer) shouldBe Unit.right()
            }

            "return not found when the abonnement does not exist" {
                coEvery { abonnementRepository.deleteAbonnement(any(), any()) } returns DeleteAbonnementError.NotFound.left()

                service.stopAbonnement(Uuid.random(), organisasjonsnummer) shouldBe StoppAbonnementError.AbonnementNotFound.left()
            }
        }
    })
