package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import arrow.core.Either
import arrow.core.right
import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainAllIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementError
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Isolate
class PostgresAbonnementRepositoryTest :
    WordSpec({
        val database = setupDatabase()

        val abonnementRepository = PostgresAbonnementRepository(database)

        "create abonnement" should {
            val abonnement =
                Abonnement(
                    Uuid.random(),
                    organisasjonsnummer = Organisasjonsnummer("889640782"),
                    identitetsnummer = Identitetsnummer("12345678910"),
                    opprettet = Clock.System.now(),
                )
            "fail if abonnement already exists" {
                abonnementRepository.opprettAbonnement(abonnement)

                abonnementRepository
                    .opprettAbonnement(abonnement)
                    .shouldBeTypeOf<Either.Left<OpprettAbonnementError.FinnesAllerede>>()
            }

            "insert a new abonnement if it does not exist" {
                abonnementRepository.opprettAbonnement(abonnement) shouldBe abonnement.right()

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer).shouldContainAllIgnoringFields(
                    listOf(abonnement),
                    Abonnement::opprettet,
                )
            }
        }

        "stop abonnement" should {
            "return unit if abonnement exists" {
                val abonnement =
                    Abonnement(
                        Uuid.random(),
                        organisasjonsnummer = Organisasjonsnummer("889640782"),
                        identitetsnummer = Identitetsnummer("12345678910"),
                        opprettet = Clock.System.now(),
                    )
                abonnementRepository.opprettAbonnement(abonnement)

                abonnementRepository.slettAbonnement(abonnement.id, abonnement.organisasjonsnummer)

                abonnementRepository.hentAbonnementer(abonnement.identitetsnummer) shouldBe emptyList()
            }

            "return unit if abonnement does not exist" {
                abonnementRepository.hentAbonnementer(Identitetsnummer("12345678910")) shouldBe emptyList()
            }
        }

        "finnesAbonnement" should {
            val abonnement =
                Abonnement(
                    Uuid.random(),
                    organisasjonsnummer = Organisasjonsnummer("889640782"),
                    identitetsnummer = Identitetsnummer("12345678910"),
                    opprettet = Clock.System.now(),
                )

            "return true for the owning organisasjonsnummer" {
                abonnementRepository.opprettAbonnement(abonnement)

                abonnementRepository.finnesAbonnement(abonnement.id, abonnement.organisasjonsnummer) shouldBe true
            }

            "return false for another organisasjonsnummer" {
                abonnementRepository.opprettAbonnement(abonnement)

                abonnementRepository.finnesAbonnement(abonnement.id, Organisasjonsnummer("974761076")) shouldBe false
            }

            "return false when the abonnement is deleted" {
                abonnementRepository.opprettAbonnement(abonnement)
                abonnementRepository.slettAbonnement(abonnement.id, abonnement.organisasjonsnummer)

                abonnementRepository.finnesAbonnement(abonnement.id, abonnement.organisasjonsnummer) shouldBe false
            }
        }
    })
