package no.nav.utenlandsadresser.application.service

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.utenlandsadresser.FastClock
import no.nav.utenlandsadresser.application.port.inbound.SlettSporingsloggError
import no.nav.utenlandsadresser.application.port.outbound.SporingsloggRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.felles.util.years
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class SporingsloggServiceTest :
    WordSpec({
        val sporingsloggRepository = mockk<SporingsloggRepository>()
        val clock = FastClock(Instant.parse("2025-01-01T12:00:00Z"))
        val service = SporingsloggService(sporingsloggRepository, clock)

        val identitetsnummer = Identitetsnummer("12345678910")
        val organisasjonsnummer = Organisasjonsnummer("974761076")

        beforeTest { clearMocks(sporingsloggRepository) }

        "skriv" should {
            "log the json with the current time as tidspunkt for utlevering" {
                coEvery { sporingsloggRepository.loggJson(any(), any(), any(), any()) } just runs

                service.skriv(identitetsnummer, organisasjonsnummer, """{"test":"test"}""")

                coVerify(exactly = 1) {
                    sporingsloggRepository.loggJson(
                        identitetsnummer,
                        organisasjonsnummer,
                        """{"test":"test"}""",
                        clock.nå,
                    )
                }
            }
        }

        "slettEldreEnn" should {
            "delete sporingslogg before now minus alder" {
                coEvery { sporingsloggRepository.slettSporingsloggerFør(any()) } just runs

                service.slettEldreEnn(10.years) shouldBe Unit.right()

                coVerify(exactly = 1) { sporingsloggRepository.slettSporingsloggerFør(clock.nå - 10.years) }
            }

            listOf(Duration.ZERO, (-1).hours).forEach { alder ->
                "reject alder $alder without deleting" {
                    service.slettEldreEnn(alder) shouldBe SlettSporingsloggError.AlderIkkePositiv.left()

                    coVerify(exactly = 0) { sporingsloggRepository.slettSporingsloggerFør(any()) }
                }
            }
        }
    })
