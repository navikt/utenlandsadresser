package no.nav.utenlandsadresser.app

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.fail
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.DeleteAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.InitAbonnementError

class AbonnementServiceTest : WordSpec({
    val abonnementRepository = mockk<AbonnementRepository>()
    val registeroppslagClient = mockk<RegisteroppslagClient>()
    val abonnementInitializer = mockk<AbonnementInitializer>()
    val abonnementService = AbonnementService(abonnementRepository, registeroppslagClient, abonnementInitializer)

    val identitetsnummer = Identitetsnummer("12345678910")
        .getOrElse { fail("Invalid fødselsnummer") }
    val organisasjonsnummer = Organisasjonsnummer("123456789")

    val utenlandsk = Postadresse.Utenlandsk(
        adresselinje1 = null,
        adresselinje2 = null,
        adresselinje3 = null,
        postnummer = null,
        poststed = null,
        landkode = Landkode(value = "UK"),
        land = Land(value = "NOR")
    )

    "start abonnement" should {
        "return error when abonnement already exist" {
            coEvery { registeroppslagClient.getPostadresse(any()) } returns utenlandsk.right()
            coEvery {
                abonnementInitializer.initAbonnement(any(), any())
            } returns InitAbonnementError.AbonnementAlreadyExists.left()

            abonnementService.startAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual StartAbonnementError.AbonnementAlreadyExists.left()
        }

        "return error when failing to get postadresse" {
            coEvery { registeroppslagClient.getPostadresse(any()) } returns GetPostadresseError.UkjentAdresse.left()

            abonnementService.startAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual StartAbonnementError.FailedToGetPostadresse.left()
        }

        "return unit when abonnement is created" {
            coEvery { registeroppslagClient.getPostadresse(any()) } returns utenlandsk.right()
            coEvery {
                abonnementInitializer.initAbonnement(any(), any())
            } returns Unit.right()

            abonnementService.startAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual Unit.right()
        }
    }

    "stop abonnement" should {
        "return error when abonnement is not found" {
            coEvery {
                abonnementRepository.deleteAbonnement(
                    identitetsnummer,
                    organisasjonsnummer
                )
            } returns DeleteAbonnementError.NotFound.left()

            abonnementService.stopAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual StoppAbonnementError.AbonnementNotFound.left()
        }

        "return unit when abonnement is stopped" {
            coEvery {
                abonnementRepository.deleteAbonnement(
                    identitetsnummer,
                    organisasjonsnummer
                )
            } returns Unit.right()

            abonnementService.stopAbonnement(identitetsnummer, organisasjonsnummer) shouldBeEqual Unit.right()
        }
    }
})
