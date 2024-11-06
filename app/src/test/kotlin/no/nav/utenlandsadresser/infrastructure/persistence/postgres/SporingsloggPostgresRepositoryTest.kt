package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
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

@DoNotParallelize
class SporingsloggPostgresRepositoryTest :
    WordSpec({
        val database = setupDatabase()

        val sporingsloggRepository = PostgresSporingsloggRepository(database)

        "loggPostadresse" should {
            "insert a new postadresse" {
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

                sporingsloggRepository.loggPostadresse(identitetsnummer, organisasjonsnummer, postadresse)

                val sporingslogger = sporingsloggRepository.getSporingslogger(identitetsnummer)

                sporingslogger.size shouldBe 1
                sporingslogger.shouldContainOnly(
                    SporingsloggDto.SporingsloggPostadresse(
                        adresselinje1 = "adresselinje1",
                        adresselinje2 = "adresselinje2",
                        adresselinje3 = "adresselinje3",
                        postnummer = "postnummer",
                        poststed = "poststed",
                        landkode = "landkode",
                        land = "land",
                    ),
                )
            }
        }
    })
