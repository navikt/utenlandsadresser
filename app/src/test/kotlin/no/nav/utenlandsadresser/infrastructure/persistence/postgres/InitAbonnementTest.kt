package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import arrow.core.Either
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainAllIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Land
import no.nav.utenlandsadresser.domain.Landkode
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import org.jetbrains.exposed.sql.Transaction
import java.util.*

@DoNotParallelize
class InitAbonnementTest :
    WordSpec({
        val database = setupDatabase()

        val abonnementRepository = PostgresAbonnementRepository(database)
        val feedRepository = spyk(PostgresFeedRepository(database))
        afterTest {
            clearMocks(feedRepository)
        }

        val initAbonnement = PostgresAbonnementInitializer(abonnementRepository, feedRepository)

        val abonnement =
            Abonnement(
                UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("889640782"),
                identitetsnummer = Identitetsnummer("12345678910"),
                opprettet = Clock.System.now(),
            )
        val postadresse =
            Postadresse.Utenlandsk(
                adresselinje1 = null,
                adresselinje2 = null,
                adresselinje3 = null,
                postnummer = null,
                poststed = null,
                landkode = Landkode(value = "UK"),
                land = Land(value = "United Kingdom"),
            )
        val feedEvent =
            FeedEvent.Outgoing(
                identitetsnummer = abonnement.identitetsnummer,
                abonnementId = abonnement.id,
                hendelsestype = Hendelsestype.OppdatertAdresse,
            )

        "init abonnement" should {
            "fail if abonnement already exists" {
                abonnementRepository.createAbonnement(abonnement)

                initAbonnement
                    .initAbonnement(abonnement, postadresse)
                    .shouldBeTypeOf<Either.Left<InitAbonnementError.AbonnementAlreadyExists>>()

                // Feed event should still be created if a postadresse is provided
                feedRepository.getFeedEvent(abonnement.organisasjonsnummer, Løpenummer(1)) shouldBe feedEvent
            }

            "rollback if createFeedEvent fails" {
                with(feedRepository) {
                    coEvery { any<Transaction>().createFeedEvent(any(), any()) } throws RuntimeException()
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
                feedRepository.getFeedEvent(abonnement.organisasjonsnummer, Løpenummer(1)) shouldBe feedEvent
            }
        }
    })
