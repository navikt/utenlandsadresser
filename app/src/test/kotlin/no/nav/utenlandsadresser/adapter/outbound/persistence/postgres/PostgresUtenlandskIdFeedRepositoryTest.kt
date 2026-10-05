package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Isolate
class PostgresUtenlandskIdFeedRepositoryTest :
    WordSpec({
        val database = setupDatabase()
        val feedRepository = PostgresUtenlandskIdFeedRepository(database, Clock.System)
        val postadresseFeedRepository = PostgresFeedRepository(database, Clock.System)

        val skatteetaten = Organisasjonsnummer("974761076")
        val annenMottaker = Organisasjonsnummer("889640782")

        fun incoming(
            organisasjonsnummer: Organisasjonsnummer,
            identitetsnummer: Identitetsnummer = Identitetsnummer("12345678910"),
        ) = UtenlandskIdFeedEvent.Incoming(
            identitetsnummer = identitetsnummer,
            abonnementId = Uuid.random(),
            hendelsestype = UtenlandskIdHendelsestype.OppdatertUtenlandskId,
            organisasjonsnummer = organisasjonsnummer,
        )

        "createFeedEvent" should {
            "store the event with løpenummer 1 for the first event" {
                val event = incoming(skatteetaten)

                feedRepository.createFeedEvent(event)

                feedRepository.hentFeedEvent(skatteetaten, Løpenummer(1)) shouldBe
                    UtenlandskIdFeedEvent.Outgoing(
                        identitetsnummer = event.identitetsnummer,
                        abonnementId = event.abonnementId,
                        hendelsestype = event.hendelsestype,
                    )
            }

            "count løpenummer separately per organisasjonsnummer" {
                val første = incoming(skatteetaten)
                val andre = incoming(skatteetaten)
                val annenEvent = incoming(annenMottaker)
                feedRepository.createFeedEvent(første)
                feedRepository.createFeedEvent(andre)
                feedRepository.createFeedEvent(annenEvent)

                feedRepository.hentFeedEvent(skatteetaten, Løpenummer(1))?.abonnementId shouldBe første.abonnementId
                feedRepository.hentFeedEvent(skatteetaten, Løpenummer(2))?.abonnementId shouldBe andre.abonnementId
                feedRepository.hentFeedEvent(skatteetaten, Løpenummer(3)).shouldBeNull()
                feedRepository.hentFeedEvent(annenMottaker, Løpenummer(1))?.abonnementId shouldBe annenEvent.abonnementId
            }

            "give unique and gapless løpenummer when events are created concurrently" {
                val antall = 25
                val identer = (1..antall).map { Identitetsnummer("1234567%04d".format(it)) }

                coroutineScope {
                    identer
                        .map { ident -> async(Dispatchers.IO) { feedRepository.createFeedEvent(incoming(skatteetaten, ident)) } }
                        .awaitAll()
                }

                val lagredeIdenter =
                    (1..antall).map { løpenummer ->
                        feedRepository.hentFeedEvent(skatteetaten, Løpenummer(løpenummer))?.identitetsnummer
                    }
                lagredeIdenter shouldContainExactlyInAnyOrder identer
                feedRepository.hentFeedEvent(skatteetaten, Løpenummer(antall + 1)).shouldBeNull()
            }

            "not add events to the postadresse feed" {
                feedRepository.createFeedEvent(incoming(skatteetaten))

                postadresseFeedRepository.hentFeedEvent(skatteetaten, Løpenummer(1)).shouldBeNull()
            }
        }
    })
