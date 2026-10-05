package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.spyk
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementMedEventError
import no.nav.utenlandsadresser.application.port.outbound.SlettAbonnementError
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Isolate
class PostgresUtenlandskIdAbonnementOppretterTest :
    WordSpec({
        val database = setupDatabase()
        val abonnementRepository = PostgresUtenlandskIdAbonnementRepository(database)
        val feedRepository = spyk(PostgresUtenlandskIdFeedRepository(database, Clock.System))
        val postadresseAbonnementRepository = PostgresAbonnementRepository(database)
        val oppretter = PostgresUtenlandskIdAbonnementOppretter(abonnementRepository, feedRepository, database)

        afterTest { clearMocks(feedRepository) }

        // Postgres lagrer mikrosekunder
        val opprettet = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        val abonnement =
            Abonnement(
                id = Uuid.random(),
                organisasjonsnummer = Organisasjonsnummer("974761076"),
                identitetsnummer = Identitetsnummer("12345678910"),
                opprettet = opprettet,
            )
        val forventetFeedEvent =
            UtenlandskIdFeedEvent.Outgoing(
                identitetsnummer = abonnement.identitetsnummer,
                abonnementId = abonnement.id,
                hendelsestype = UtenlandskIdHendelsestype.OppdatertUtenlandskId,
            )

        "opprettMedEvent" should {
            "create abonnement and feed event when the person has utenlandsk id" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = true) shouldBe abonnement.right()

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer) shouldContainExactly listOf(abonnement)
                feedRepository.hentFeedEvent(abonnement.organisasjonsnummer, Løpenummer(1)) shouldBe forventetFeedEvent
            }

            "create abonnement without feed event when the person has no utenlandsk id" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = false) shouldBe abonnement.right()

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer) shouldContainExactly listOf(abonnement)
                feedRepository.hentFeedEvent(abonnement.organisasjonsnummer, Løpenummer(1)).shouldBeNull()
            }

            "return the existing abonnement and still create a feed event when it already exists" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = false)
                val nyttForsøk = abonnement.copy(id = Uuid.random())

                oppretter.opprettMedEvent(nyttForsøk, harUtenlandskId = true) shouldBe
                    OpprettAbonnementMedEventError.AbonnementFinnesAllerede(abonnement).left()

                // Hendelsen skal peke på det eksisterende abonnementet
                feedRepository.hentFeedEvent(abonnement.organisasjonsnummer, Løpenummer(1)) shouldBe forventetFeedEvent
            }

            "roll back the abonnement when the feed event cannot be created" {
                coEvery { feedRepository.createFeedEvent(any(), any()) } throws RuntimeException("feil")

                shouldThrow<RuntimeException> {
                    oppretter.opprettMedEvent(abonnement, harUtenlandskId = true)
                }

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer).shouldBeEmpty()
            }

            "not create a postadresse abonnement" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = true)

                postadresseAbonnementRepository.hentAbonnementer(abonnement.identitetsnummer).shouldBeEmpty()
            }
        }

        "slettAbonnement" should {
            "delete the abonnement for the owning organisasjonsnummer" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = false)

                abonnementRepository.slettAbonnement(abonnement.id, abonnement.organisasjonsnummer) shouldBe Unit.right()

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer).shouldBeEmpty()
            }

            "not delete an abonnement owned by another organisasjonsnummer" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = false)

                abonnementRepository.slettAbonnement(abonnement.id, Organisasjonsnummer("889640782")) shouldBe
                    SlettAbonnementError.IkkeFunnet.left()

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer) shouldContainExactly listOf(abonnement)
            }

            "keep feed events when the abonnement is deleted" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = true)

                abonnementRepository.slettAbonnement(abonnement.id, abonnement.organisasjonsnummer)

                feedRepository.hentFeedEvent(abonnement.organisasjonsnummer, Løpenummer(1)) shouldBe forventetFeedEvent
            }
        }

        "finnesAbonnement" should {
            "return true for the owning organisasjonsnummer" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = false)

                abonnementRepository.finnesAbonnement(abonnement.id, abonnement.organisasjonsnummer) shouldBe true
            }

            "return false for another organisasjonsnummer" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = false)

                abonnementRepository.finnesAbonnement(abonnement.id, Organisasjonsnummer("889640782")) shouldBe false
            }

            "return false when the abonnement is deleted" {
                oppretter.opprettMedEvent(abonnement, harUtenlandskId = false)
                abonnementRepository.slettAbonnement(abonnement.id, abonnement.organisasjonsnummer)

                abonnementRepository.finnesAbonnement(abonnement.id, abonnement.organisasjonsnummer) shouldBe false
            }

            "return false for a postadresse abonnement with the same id" {
                postadresseAbonnementRepository.opprettAbonnement(abonnement)

                abonnementRepository.finnesAbonnement(abonnement.id, abonnement.organisasjonsnummer) shouldBe false
            }
        }
    })
