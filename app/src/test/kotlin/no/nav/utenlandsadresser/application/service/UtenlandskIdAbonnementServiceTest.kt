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
import no.nav.utenlandsadresser.application.port.inbound.StartUtenlandskIdAbonnementError
import no.nav.utenlandsadresser.application.port.inbound.StoppAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementMedEventError
import no.nav.utenlandsadresser.application.port.outbound.SlettAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementOppretter
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdOppslag
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
        val utenlandskIdOppslag = mockk<UtenlandskIdOppslag>()
        val oppretter = mockk<UtenlandskIdAbonnementOppretter>()
        val service = UtenlandskIdAbonnementService(abonnementRepository, utenlandskIdOppslag, oppretter, FastClock())

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

        "start" should {
            "create a feed event when the person has utenlandsk id" {
                coEvery { utenlandskIdOppslag.hentUtenlandskIdentitet(identitetsnummer) } returns listOf(utenlandskIdentitet).right()
                coEvery { oppretter.opprettMedEvent(any(), any()) } answers { firstArg<Abonnement>().right() }

                val result = service.start(identitetsnummer, organisasjonsnummer)

                result.isRight() shouldBe true
                coVerify(exactly = 1) {
                    oppretter.opprettMedEvent(
                        match { it.identitetsnummer == identitetsnummer && it.organisasjonsnummer == organisasjonsnummer },
                        harUtenlandskId = true,
                    )
                }
            }

            "not create a feed event when the person has no utenlandsk id" {
                coEvery { utenlandskIdOppslag.hentUtenlandskIdentitet(identitetsnummer) } returns emptyList<UtenlandskIdentitet>().right()
                coEvery { oppretter.opprettMedEvent(any(), any()) } answers { firstArg<Abonnement>().right() }

                service.start(identitetsnummer, organisasjonsnummer).isRight() shouldBe true

                coVerify(exactly = 1) { oppretter.opprettMedEvent(any(), harUtenlandskId = false) }
            }

            "not create the abonnement when the lookup fails" {
                coEvery { utenlandskIdOppslag.hentUtenlandskIdentitet(identitetsnummer) } returns
                    UtenlandskIdOppslag.Error.Kommunikasjonsfeil.left()

                service.start(identitetsnummer, organisasjonsnummer) shouldBe
                    StartUtenlandskIdAbonnementError.KunneIkkeHenteUtenlandskId.left()

                coVerify(exactly = 0) { oppretter.opprettMedEvent(any(), any()) }
            }

            "return the existing abonnement when it already exists" {
                coEvery { utenlandskIdOppslag.hentUtenlandskIdentitet(identitetsnummer) } returns listOf(utenlandskIdentitet).right()
                coEvery { oppretter.opprettMedEvent(any(), any()) } returns
                    OpprettAbonnementMedEventError.AbonnementFinnesAllerede(eksisterendeAbonnement).left()

                service.start(identitetsnummer, organisasjonsnummer) shouldBe
                    StartUtenlandskIdAbonnementError.AbonnementFinnesAllerede(eksisterendeAbonnement).left()
            }
        }

        "stopp" should {
            "stop the abonnement" {
                coEvery { abonnementRepository.slettAbonnement(eksisterendeAbonnement.id, organisasjonsnummer) } returns Unit.right()

                service.stopp(eksisterendeAbonnement.id, organisasjonsnummer) shouldBe Unit.right()
            }

            "return not found when the abonnement does not exist" {
                coEvery { abonnementRepository.slettAbonnement(any(), any()) } returns SlettAbonnementError.IkkeFunnet.left()

                service.stopp(Uuid.random(), organisasjonsnummer) shouldBe StoppAbonnementError.AbonnementIkkeFunnet.left()
            }
        }
    })
