package no.nav.utenlandsadresser.app

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.fail
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.CreateAbonnementError

class AbonnementServiceTest : WordSpec({
    val abonnementRepository = mockk<AbonnementRepository>()
    val abonnementService = AbonnementService(abonnementRepository)

    val identitetsnummer = Identitetsnummer("12345678910")
        .getOrElse { fail("Invalid fødselsnummer") }
    val organisasjonsnummer = Organisasjonsnummer("123456789")

    "start abonnement" should {
        "return error when abonnement already exist" {
            every { abonnementRepository.createAbonnement(any()) } returns CreateAbonnementError.AlreadyExists.left()

            abonnementService.startAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual AbonnementService.StartAbonnementError.AbonnementAlreadyExists.left()
        }

        "return unit when abonnement exists" {
            every { abonnementRepository.createAbonnement(any()) } returns Unit.right()

            abonnementService.startAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual Unit.right()
        }
    }

    "stop abonnement" should {
        "return unit when abonnement is stopped" {
            every { abonnementRepository.deleteAbonnement(identitetsnummer, organisasjonsnummer) } returns Unit

            abonnementService.stopAbonnement(identitetsnummer, organisasjonsnummer) shouldBeEqual Unit
        }
    }
})
