package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainAllIgnoringFields
import io.kotest.matchers.shouldBe
import kotest.extension.setupDatabase
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.CreateAbonnementError

class AbonnementExposedRepositoryTest : WordSpec({
    val database = setupDatabase()

    val abonnementRepository = AbonnementPostgresRepository(database)

    "create abonnement" should {
        val abonnement = Abonnement(
            organisasjonsnummer = Organisasjonsnummer("889640782"),
            identitetsnummer = Identitetsnummer("12345678910"),
            opprettet = Clock.System.now(),
        )
        "fail if abonnement already exists" {
            abonnementRepository.createAbonnement(abonnement)

            abonnementRepository.createAbonnement(abonnement) shouldBe CreateAbonnementError.AlreadyExists.left()
        }

        "insert a new abonnement if it does not exist" {
            abonnementRepository.createAbonnement(abonnement) shouldBe Unit.right()

            abonnementRepository.getAbonnementer(abonnement.identitetsnummer).shouldContainAllIgnoringFields(
                listOf(abonnement),
                Abonnement::opprettet,
            )
        }
    }

    "stop abonnement" should {
        "return unit if abonnement exists" {
            val abonnement = Abonnement(
                organisasjonsnummer = Organisasjonsnummer("889640782"),
                identitetsnummer = Identitetsnummer("12345678910"),
                opprettet = Clock.System.now(),
            )
            abonnementRepository.createAbonnement(abonnement)

            abonnementRepository.deleteAbonnement(abonnement.identitetsnummer, abonnement.organisasjonsnummer)

            abonnementRepository.getAbonnementer(abonnement.identitetsnummer) shouldBe emptyList()
        }

        "return unit if abonnement does not exist" {
            val abonnement = Abonnement(
                organisasjonsnummer = Organisasjonsnummer("889640782"),
                identitetsnummer = Identitetsnummer("12345678910"),
                opprettet = Clock.System.now(),
            )

            abonnementRepository.deleteAbonnement(abonnement.identitetsnummer, abonnement.organisasjonsnummer)

            abonnementRepository.getAbonnementer(abonnement.identitetsnummer) shouldBe emptyList()
        }
    }
})
