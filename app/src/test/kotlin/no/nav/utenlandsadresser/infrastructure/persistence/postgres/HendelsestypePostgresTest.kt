package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.Hendelsestype

class HendelsestypePostgresTest :
    WordSpec({
        "fromDomain" should {
            "map gradert adressebeskyttelse to ADRESSEBESKYTTELSE" {
                HendelsestypePostgres.fromDomain(
                    Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT),
                ) shouldBe HendelsestypePostgres.ADRESSEBESKYTTELSE
            }

            "map ugradert adressebeskyttelse to OPPDATERT_ADRESSE" {
                HendelsestypePostgres.fromDomain(
                    Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT),
                ) shouldBe HendelsestypePostgres.OPPDATERT_ADRESSE
            }

            "map OppdatertAdresse to OPPDATERT_ADRESSE" {
                HendelsestypePostgres.fromDomain(Hendelsestype.OppdatertAdresse) shouldBe
                    HendelsestypePostgres.OPPDATERT_ADRESSE
            }
        }

        "toDomain" should {
            "map ADRESSEBESKYTTELSE to gradert adressebeskyttelse" {
                HendelsestypePostgres.ADRESSEBESKYTTELSE.toDomain() shouldBe
                    Hendelsestype.Adressebeskyttelse(AdressebeskyttelseGradering.GRADERT)
            }

            "map OPPDATERT_ADRESSE to OppdatertAdresse" {
                HendelsestypePostgres.OPPDATERT_ADRESSE.toDomain() shouldBe Hendelsestype.OppdatertAdresse
            }
        }

        "persisted ordinals" should {
            "keep the values used by the database" {
                // Kolonnen lagrer ordinalen, så endret rekkefølge gjør at lagrede rader tolkes feil.
                HendelsestypePostgres.ADRESSEBESKYTTELSE.ordinal shouldBe 0
                HendelsestypePostgres.OPPDATERT_ADRESSE.ordinal shouldBe 1
            }
        }
    })
