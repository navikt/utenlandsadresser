package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import arrow.core.Either
import arrow.core.right
import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainAllIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.CreateAbonnementError
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import java.util.*

@DoNotParallelize
class AbonnementPostgresRepositoryTest :
    WordSpec({
        val database = setupDatabase()

        val abonnementRepository = AbonnementPostgresRepository(database)

        "create abonnement" should {
            val abonnement =
                Abonnement(
                    UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("889640782"),
                    identitetsnummer = Identitetsnummer("12345678910"),
                    opprettet = Clock.System.now(),
                )
            "fail if abonnement already exists" {
                abonnementRepository.createAbonnement(abonnement)

                abonnementRepository
                    .createAbonnement(abonnement)
                    .shouldBeTypeOf<Either.Left<CreateAbonnementError.AlreadyExists>>()
            }

            "insert a new abonnement if it does not exist" {
                abonnementRepository.createAbonnement(abonnement) shouldBe abonnement.right()

                abonnementRepository.getAbonnementer(abonnement.identitetsnummer).shouldContainAllIgnoringFields(
                    listOf(abonnement),
                    Abonnement::opprettet,
                )
            }
        }

        "stop abonnement" should {
            "return unit if abonnement exists" {
                val abonnement =
                    Abonnement(
                        UUID.randomUUID(),
                        organisasjonsnummer = Organisasjonsnummer("889640782"),
                        identitetsnummer = Identitetsnummer("12345678910"),
                        opprettet = Clock.System.now(),
                    )
                abonnementRepository.createAbonnement(abonnement)

                abonnementRepository.deleteAbonnement(abonnement.id, abonnement.organisasjonsnummer)

                abonnementRepository.getAbonnementer(abonnement.identitetsnummer) shouldBe emptyList()
            }

            "return unit if abonnement does not exist" {
                abonnementRepository.getAbonnementer(Identitetsnummer("12345678910")) shouldBe emptyList()
            }
        }
    })
