package no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json

import io.kotest.core.spec.style.WordSpec
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val jsonFormat =
    Json {
        prettyPrint = true
    }

class PersonendringJsonTest :
    WordSpec({
        "PersonendringJson" should {
            "serialize til JSON" {
                val personendringJson =
                    PersonendringJson(
                        listOf(
                            PersonopplysningJson(
                                endringstype = EndringstypeJson.OPPRETT,
                                ident = "12345678910",
                                opplysningstype = OpplysningstypeJson.UTENLANDSK_KONTAKTADRESSE,
                                endringsmelding =
                                    EndringsmeldingJson.Kontaktadresse(
                                        kilde = "Skatteetaten",
                                        gyldigFraOgMed =
                                            Clock.System
                                                .now()
                                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                                .date,
                                        gyldigTilOgMed =
                                            Clock.System
                                                .now()
                                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                                .date
                                                .plus(1, DateTimeUnit.YEAR),
                                        coAdressenavn = null,
                                        adresse =
                                            UtenlandskAdresseJson.UtenlandskAdresse(
                                                adressenavnNummer = "Testgate 1",
                                                bygningEtasjeLeilighet = "Etasje 1",
                                                postboksNummerNavn = null,
                                                postkode = "1234",
                                                bySted = "By",
                                                regionDistriktOmraade = "Region",
                                                landkode = "NO",
                                            ),
                                    ),
                                opplysningsId = null,
                            ),
                        ),
                    )

                val json =
                    jsonFormat.encodeToString(personendringJson)
                println(json)
            }
        }
    })
