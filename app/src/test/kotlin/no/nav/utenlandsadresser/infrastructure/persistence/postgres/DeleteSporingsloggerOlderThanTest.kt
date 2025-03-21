package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import io.kotest.core.spec.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.Adresselinje
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Land
import no.nav.utenlandsadresser.domain.Landkode
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.domain.Postnummer
import no.nav.utenlandsadresser.domain.Poststed
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.SporingsloggDto
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import no.nav.utenlandsadresser.util.years
import kotlin.time.Duration.Companion.days

@DoNotParallelize
class DeleteSporingsloggerOlderThanTest :
    WordSpec({
        val database = setupDatabase()

        val sporingsloggRepository = PostgresSporingsloggRepository(database)

        val identitetsnummer = Identitetsnummer("12345678910")
        val organisasjonsnummer = Organisasjonsnummer("889640782")
        val postadresse =
            Postadresse.Utenlandsk(
                adresselinje1 = Adresselinje("adresselinje1"),
                adresselinje2 = Adresselinje("adresselinje2"),
                adresselinje3 = Adresselinje("adresselinje3"),
                postnummer = Postnummer("postnummer"),
                poststed = Poststed("poststed"),
                landkode = Landkode("landkode"),
                land = Land("land"),
            )
        "delete sporingslogg older than duration" should {
            "delete sporingslogg older than duration" {
                sporingsloggRepository.loggPostadresse(
                    identitetsnummer,
                    organisasjonsnummer,
                    postadresse,
                    tidspunktForUtlevering = Clock.System.now().minus(10.years + 1.days),
                )

                val loggetPostadresse = sporingsloggRepository.getSporingslogger(identitetsnummer, organisasjonsnummer)
                // Verifiser at det finnes data som kan slettes
                loggetPostadresse.shouldContainOnly(
                    SporingsloggDto.SporingsloggPostadresse
                        .fromDomain(postadresse)
                        .encodeToJsonElement(),
                )

                sporingsloggRepository.deleteSporingsloggerOlderThan(10.years)

                val sporingslogger = sporingsloggRepository.getSporingslogger(identitetsnummer, organisasjonsnummer)

                sporingslogger.shouldBeEmpty()
            }

            "not delete sporingslogg younger than duration" {
                sporingsloggRepository.loggPostadresse(
                    identitetsnummer,
                    organisasjonsnummer,
                    postadresse,
                    tidspunktForUtlevering = Clock.System.now().minus(10.years - 1.days),
                )

                val loggedPostadresse = sporingsloggRepository.getSporingslogger(identitetsnummer, organisasjonsnummer)

                loggedPostadresse.size shouldBe 1

                sporingsloggRepository.deleteSporingsloggerOlderThan(10.years)

                val sporingslogger = sporingsloggRepository.getSporingslogger(identitetsnummer, organisasjonsnummer)

                sporingslogger.size shouldBe 1
            }
        }
    })
