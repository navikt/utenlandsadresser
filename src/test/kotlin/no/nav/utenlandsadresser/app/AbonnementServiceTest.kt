package no.nav.utenlandsadresser.app

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.DeleteAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.InitAbonnementError
import java.util.*

class AbonnementServiceTest : WordSpec({
    val abonnementRepository = mockk<AbonnementRepository>()
    val registeroppslagClient = mockk<RegisteroppslagClient>()
    val abonnementInitializer = mockk<AbonnementInitializer>()
    val abonnementService = AbonnementService(abonnementRepository, registeroppslagClient, abonnementInitializer)

    val identitetsnummer = Identitetsnummer("12345678910")
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

    val abonnementId = UUID.randomUUID()
    val abonnement = Abonnement(
        id = abonnementId,
        identitetsnummer = identitetsnummer,
        organisasjonsnummer = organisasjonsnummer,
        opprettet = Clock.System.now()
    )

    "start abonnement" should {
        "return error when abonnement already exist" {
            coEvery { registeroppslagClient.getPostadresse(any()) } returns utenlandsk.right()
            coEvery {
                abonnementInitializer.initAbonnement(any(), any())
            } returns InitAbonnementError.AbonnementAlreadyExists(abonnement).left()

            abonnementService.startAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual StartAbonnementError.AbonnementAlreadyExists(abonnement).left()
        }

        "return error when failing to get postadresse" {
            coEvery { registeroppslagClient.getPostadresse(any()) } returns GetPostadresseError.UgyldigForespørsel.left()

            abonnementService.startAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual StartAbonnementError.FailedToGetPostadresse.left()
        }

        "return abonnement when abonnement is created" {
            coEvery { registeroppslagClient.getPostadresse(any()) } returns utenlandsk.right()
            coEvery {
                abonnementInitializer.initAbonnement(any(), any())
            } returns abonnement.right()

            abonnementService.startAbonnement(
                identitetsnummer,
                organisasjonsnummer
            ) shouldBeEqual abonnement.right()
        }
    }

    "stop abonnement" should {
        "return error when abonnement is not found" {
            coEvery {
                abonnementRepository.deleteAbonnement(
                    abonnementId,
                    organisasjonsnummer
                )
            } returns DeleteAbonnementError.NotFound.left()

            abonnementService.stopAbonnement(
                abonnementId,
                organisasjonsnummer
            ) shouldBeEqual StoppAbonnementError.AbonnementNotFound.left()
        }

        "return unit when abonnement is stopped" {
            coEvery {
                abonnementRepository.deleteAbonnement(
                    abonnementId,
                    organisasjonsnummer
                )
            } returns Unit.right()

            abonnementService.stopAbonnement(abonnementId, organisasjonsnummer) shouldBeEqual Unit.right()
        }
    }
})
