package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainAllIgnoringFields
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.spyk
import kotest.extension.setupDatabase
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.*
import org.jetbrains.exposed.sql.Transaction
import java.util.*

class InitAbonnementTest : WordSpec({
    val database = setupDatabase()

    val abonnementRepository = AbonnementPostgresRepository(database)
    val feedRepository = spyk(FeedPostgresRepository(database))
    afterTest {
        clearMocks(feedRepository)
    }

    val initAbonnement = PostgresAbonnementInitializer(abonnementRepository, feedRepository)

    val abonnement = Abonnement(
        UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("889640782"),
        identitetsnummer = Identitetsnummer("12345678910"),
        opprettet = Clock.System.now()
    )
    val postadresse = Postadresse.Utenlandsk(
        adresselinje1 = null,
        adresselinje2 = null,
        adresselinje3 = null,
        postnummer = null,
        poststed = null,
        landkode = Landkode(value = "UK"),
        land = Land(value = "United Kingdom"),
    )

    "init abonnement" should {
        "fail if abonnement already exists" {
            abonnementRepository.createAbonnement(abonnement)

            initAbonnement.initAbonnement(abonnement, null) shouldBe InitAbonnementError.AbonnementAlreadyExists(abonnement).left()
        }

        "rollback if createFeedEvent fails" {
            with(feedRepository) {
                coEvery { any<Transaction>().createFeedEvent(any()) } throws RuntimeException()
            }
            shouldThrow<RuntimeException> {
                initAbonnement.initAbonnement(abonnement, postadresse)
            }

            abonnementRepository.getAbonnementer(abonnement.identitetsnummer) shouldBe emptyList()
        }

        "create a new abonnement if it does not exist" {
            initAbonnement.initAbonnement(abonnement, postadresse) shouldBe abonnement.right()

            abonnementRepository.getAbonnementer(abonnement.identitetsnummer).shouldContainAllIgnoringFields(
                listOf(abonnement),
                Abonnement::opprettet,
            )
        }
    }
})
