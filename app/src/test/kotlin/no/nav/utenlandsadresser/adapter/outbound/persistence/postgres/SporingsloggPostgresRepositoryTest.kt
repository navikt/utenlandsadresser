package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import no.nav.utenlandsadresser.domain.Adresselinje
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Iso3166Alpha3
import no.nav.utenlandsadresser.domain.Land
import no.nav.utenlandsadresser.domain.Landkode
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.domain.Postnummer
import no.nav.utenlandsadresser.domain.Poststed
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetKilde
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetsnummer
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import kotlin.time.Clock

@Isolate
class SporingsloggPostgresRepositoryTest :
    WordSpec({
        val database = setupDatabase()

        val sporingsloggRepository = PostgresSporingsloggRepository(database, Clock.System)

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

                sporingsloggRepository.loggPostadresse(identitetsnummer, organisasjonsnummer, postadresse, Clock.System.now())

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

                sporingsloggRepository.loggJson(identitetsnummer, organisasjonsnummer, jsonElement, Clock.System.now())

                val sporingslogger = sporingsloggRepository.getSporingslogger(identitetsnummer, organisasjonsnummer)

                sporingslogger.size shouldBe 1
                sporingslogger.shouldContainOnly(jsonElement)
            }
        }

        "loggUtenlandskId" should {
            "store the delivered utenlandske id-er under a descriptive key" {
                val identitetsnummer = Identitetsnummer("12345678910")
                val organisasjonsnummer = Organisasjonsnummer("974761076")
                val utenlandskIdentitet =
                    UtenlandskIdentitet(
                        identitetsnummer = UtenlandskIdentitetsnummer("123010190B456"),
                        utstederland = Iso3166Alpha3("DEU"),
                        kilde = UtenlandskIdentitetKilde("Dolly"),
                    )

                sporingsloggRepository.loggUtenlandskId(
                    identitetsnummer,
                    organisasjonsnummer,
                    listOf(utenlandskIdentitet),
                    Clock.System.now(),
                )

                sporingsloggRepository.getSporingslogger(identitetsnummer, organisasjonsnummer).shouldContainOnly(
                    Json.parseToJsonElement(
                        """
                        {
                          "utenlandskId": [
                            {"identitetsnummer": "123010190B456", "utstederland": "DEU", "kilde": "Dolly"}
                          ]
                        }
                        """.trimIndent(),
                    ),
                )
            }
        }
    })
