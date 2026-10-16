package no.nav.utenlandsadresser.infrastructure.kafka

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.utenlandsadresser.domain.Identitetsnummer
import kotlin.time.Clock
import kotlin.time.toJavaInstant

class PersonhendelseToDomainTest :
    WordSpec({
        fun personhendelse(
            opplysningstype: String,
            personidenter: List<String> = listOf("01010100001"),
            adressebeskyttelse: Gradering? = null,
        ): Personhendelse =
            Personhendelse
                .newBuilder()
                .apply {
                    hendelseId = "1"
                    master = "PDL"
                    opprettet = Clock.System.now().toJavaInstant()
                    endringstype = Endringstype.OPPRETTET
                    this.personidenter = personidenter
                    this.opplysningstype = opplysningstype
                    this.adressebeskyttelse = adressebeskyttelse?.let { Adressebeskyttelse(it) }
                }.build()

        "Personhendelse.toDomain" should {
            "convert BOSTEDSADRESSE_V1 to a bostedsadresse event" {
                personhendelse("BOSTEDSADRESSE_V1").toDomain() shouldBe
                    Livshendelse.Bostedsadresse(listOf(Identitetsnummer("01010100001")))
            }

            "convert KONTAKTADRESSE_V1 to a kontaktadresse event" {
                personhendelse("KONTAKTADRESSE_V1").toDomain() shouldBe
                    Livshendelse.Kontaktadresse(listOf(Identitetsnummer("01010100001")))
            }

            "preserve STRENGT_FORTROLIG_UTLAND adressebeskyttelse" {
                personhendelse("ADRESSEBESKYTTELSE_V1", adressebeskyttelse = Gradering.STRENGT_FORTROLIG_UTLAND)
                    .toDomain() shouldBe
                    Livshendelse.Adressebeskyttelse(
                        listOf(Identitetsnummer("01010100001")),
                        Gradering.STRENGT_FORTROLIG_UTLAND,
                    )
            }

            "preserve STRENGT_FORTROLIG adressebeskyttelse" {
                personhendelse("ADRESSEBESKYTTELSE_V1", adressebeskyttelse = Gradering.STRENGT_FORTROLIG)
                    .toDomain() shouldBe
                    Livshendelse.Adressebeskyttelse(
                        listOf(Identitetsnummer("01010100001")),
                        Gradering.STRENGT_FORTROLIG,
                    )
            }

            "preserve FORTROLIG adressebeskyttelse" {
                personhendelse("ADRESSEBESKYTTELSE_V1", adressebeskyttelse = Gradering.FORTROLIG)
                    .toDomain() shouldBe
                    Livshendelse.Adressebeskyttelse(
                        listOf(Identitetsnummer("01010100001")),
                        Gradering.FORTROLIG,
                    )
            }

            "preserve UGRADERT adressebeskyttelse" {
                personhendelse("ADRESSEBESKYTTELSE_V1", adressebeskyttelse = Gradering.UGRADERT)
                    .toDomain() shouldBe
                    Livshendelse.Adressebeskyttelse(
                        listOf(Identitetsnummer("01010100001")),
                        Gradering.UGRADERT,
                    )
            }

            "use UGRADERT when adressebeskyttelse is null" {
                personhendelse("ADRESSEBESKYTTELSE_V1").toDomain() shouldBe
                    Livshendelse.Adressebeskyttelse(
                        listOf(Identitetsnummer("01010100001")),
                        Gradering.UGRADERT,
                    )
            }

            "return null for an unknown opplysningstype" {
                personhendelse("DOEDSFALL_V1").toDomain() shouldBe null
            }

            "trim whitespace around opplysningstype" {
                personhendelse(" BOSTEDSADRESSE_V1 ").toDomain() shouldBe
                    Livshendelse.Bostedsadresse(listOf(Identitetsnummer("01010100001")))
            }

            "preserve all personidenter in order" {
                personhendelse(
                    "BOSTEDSADRESSE_V1",
                    personidenter = listOf("01010100001", "01010100002"),
                ).toDomain() shouldBe
                    Livshendelse.Bostedsadresse(
                        listOf(Identitetsnummer("01010100001"), Identitetsnummer("01010100002")),
                    )
            }
        }
    })
