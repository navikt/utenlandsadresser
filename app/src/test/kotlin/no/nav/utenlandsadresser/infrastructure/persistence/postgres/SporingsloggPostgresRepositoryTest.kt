package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import no.nav.utenlandsadresser.domain.Adresselinje
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Land
import no.nav.utenlandsadresser.domain.Landkode
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.domain.Postnummer
import no.nav.utenlandsadresser.domain.Poststed
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

                val sporingslogger = sporingsloggRepository.getSporingslogger(identitetsnummer, organisasjonsnummer)

                sporingslogger.size shouldBe 1
                sporingslogger.shouldContainOnly(
                    buildJsonObject {
                        put("adresselinje1", JsonPrimitive("adresselinje1"))
                        put("adresselinje2", JsonPrimitive("adresselinje2"))
                        put("adresselinje3", JsonPrimitive("adresselinje3"))
                        put("postnummer", JsonPrimitive("postnummer"))
                        put("poststed", JsonPrimitive("poststed"))
                        put("landkode", JsonPrimitive("landkode"))
                        put("land", JsonPrimitive("land"))
                    },
                )
            }

            "insert a new json sporingslogg" {
                val identitetsnummer = Identitetsnummer("12345678910")
                val organisasjonsnummer = Organisasjonsnummer("889640782")
                val jsonElement =
                    buildJsonObject {
                        put("anyKey", JsonPrimitive("anyValue"))
                    }

                sporingsloggRepository.loggJson(identitetsnummer, organisasjonsnummer, jsonElement)

                val sporingslogger = sporingsloggRepository.getSporingslogger(identitetsnummer, organisasjonsnummer)

                sporingslogger.size shouldBe 1
                sporingslogger.shouldContainOnly(jsonElement)
            }
        }
    })
