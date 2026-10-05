package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import arrow.core.Either
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainAllIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.spyk
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementMedEventError
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
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Isolate
class PostgresAbonnementOppretterTest :
    WordSpec({
        val database = setupDatabase()

        val abonnementRepository = PostgresAbonnementRepository(database)
        val feedRepository = spyk(PostgresFeedRepository(database, Clock.System))
        afterTest {
            clearMocks(feedRepository)
        }

        val abonnementOppretter = PostgresAbonnementOppretter(abonnementRepository, feedRepository, database)

        val abonnement =
            Abonnement(
                Uuid.random(),
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

        "opprettMedEvent" should {
            "fail if abonnement already exists" {
                abonnementRepository.opprettAbonnement(abonnement)

                abonnementOppretter
                    .opprettMedEvent(abonnement, postadresse)
                    .shouldBeTypeOf<Either.Left<OpprettAbonnementMedEventError.AbonnementFinnesAllerede>>()

                // Feed event should still be created if a postadresse is provided
                feedRepository.hentFeedEvent(abonnement.organisasjonsnummer, Løpenummer(1)) shouldBe feedEvent
            }

            "rollback if createFeedEvent fails" {
                coEvery { feedRepository.createFeedEvent(any(), any()) } throws RuntimeException()
                shouldThrow<RuntimeException> {
                    abonnementOppretter.opprettMedEvent(abonnement, postadresse)
                }

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer) shouldBe emptyList()
            }

            "create a new abonnement if it does not exist" {
                abonnementOppretter.opprettMedEvent(abonnement, postadresse) shouldBe abonnement.right()

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer).shouldContainAllIgnoringFields(
                    listOf(abonnement),
                    Abonnement::opprettet,
                )
                feedRepository.hentFeedEvent(abonnement.organisasjonsnummer, Løpenummer(1)) shouldBe feedEvent
            }
        }
    })
