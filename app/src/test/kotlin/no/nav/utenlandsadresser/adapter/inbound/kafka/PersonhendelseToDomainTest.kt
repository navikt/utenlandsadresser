package no.nav.utenlandsadresser.adapter.inbound.kafka

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Livshendelse
import kotlin.time.Clock
import kotlin.time.toJavaInstant

class PersonhendelseToDomainTest :
    WordSpec({
        val identitetsnummer = Identitetsnummer("12345678910")

        fun personhendelse(
            opplysningstype: String,
            adressebeskyttelse: Adressebeskyttelse? = null,
        ): Personhendelse =
            Personhendelse
                .newBuilder()
                .apply {
                    hendelseId = "0"
                    master = "PDL"
                    opprettet = Clock.System.now().toJavaInstant()
                    endringstype = Endringstype.OPPRETTET
                    personidenter = listOf(identitetsnummer.value)
                    this.opplysningstype = opplysningstype
                    this.adressebeskyttelse = adressebeskyttelse
                }.build()

        "toDomain" should {
            "mappe bostedsadresse" {
                personhendelse("BOSTEDSADRESSE_V1").toDomain() shouldBe
                    Livshendelse.Bostedsadresse(listOf(identitetsnummer))
            }

            "mappe kontaktadresse" {
                personhendelse("KONTAKTADRESSE_V1").toDomain() shouldBe
                    Livshendelse.Kontaktadresse(listOf(identitetsnummer))
            }

            "ignorere andre opplysningstyper" {
                personhendelse("NAVN_V1").toDomain().shouldBeNull()
            }

            listOf(
                Gradering.STRENGT_FORTROLIG_UTLAND to Livshendelse.Adressebeskyttelse.Gradering.STRENGT_FORTROLIG_UTLAND,
                Gradering.STRENGT_FORTROLIG to Livshendelse.Adressebeskyttelse.Gradering.STRENGT_FORTROLIG,
                Gradering.FORTROLIG to Livshendelse.Adressebeskyttelse.Gradering.FORTROLIG,
                Gradering.UGRADERT to Livshendelse.Adressebeskyttelse.Gradering.UGRADERT,
            ).forEach { (avro, domene) ->
                "mappe adressebeskyttelse $avro til $domene" {
                    personhendelse("ADRESSEBESKYTTELSE_V1", Adressebeskyttelse(avro)).toDomain() shouldBe
                        Livshendelse.Adressebeskyttelse(listOf(identitetsnummer), domene)
                }
            }

            "tolke manglende adressebeskyttelse som ugradert" {
                personhendelse("ADRESSEBESKYTTELSE_V1").toDomain() shouldBe
                    Livshendelse.Adressebeskyttelse(
                        listOf(identitetsnummer),
                        Livshendelse.Adressebeskyttelse.Gradering.UGRADERT,
                    )
            }
        }
    })
