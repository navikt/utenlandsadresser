package no.nav.utenlandsadresser.application.service

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.utenlandsadresser.application.port.outbound.AbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.FeedRepository
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.AdressebeskyttelseGradering
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Livshendelse
import no.nav.utenlandsadresser.domain.Livshendelse.Adressebeskyttelse.Gradering
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class LivshendelseServiceTest :
    WordSpec({
        val abonnementRepository = mockk<AbonnementRepository>()
        val feedRepository = mockk<FeedRepository>()
        val service = LivshendelseService(abonnementRepository, feedRepository)

        val fødselsnummer = Identitetsnummer("12345678910")
        val dNummer = Identitetsnummer("41345678910")
        val skatteetaten = Organisasjonsnummer("974761076")
        val annenMottaker = Organisasjonsnummer("889640782")

        fun abonnement(
            identitetsnummer: Identitetsnummer,
            organisasjonsnummer: Organisasjonsnummer,
        ) = Abonnement(
            id = Uuid.random(),
            organisasjonsnummer = organisasjonsnummer,
            identitetsnummer = identitetsnummer,
            opprettet = Clock.System.now(),
        )

        fun Abonnement.event(hendelsestype: Hendelsestype) =
            FeedEvent.Incoming(
                identitetsnummer = identitetsnummer,
                abonnementId = id,
                hendelsestype = hendelsestype,
                organisasjonsnummer = organisasjonsnummer,
            )

        beforeTest {
            clearMocks(abonnementRepository, feedRepository)
            coEvery { feedRepository.opprettUtenDuplikater(any(), any()) } just runs
        }

        "håndter" should {
            "legge én hendelse på feeden per abonnement, for alle identene til personen" {
                val abonnementer = listOf(abonnement(fødselsnummer, skatteetaten), abonnement(dNummer, annenMottaker))
                coEvery { abonnementRepository.hentAbonnementer(listOf(fødselsnummer, dNummer)) } returns abonnementer

                service.håndter(Livshendelse.Bostedsadresse(listOf(fødselsnummer, dNummer)))

                coVerify(exactly = 1) {
                    feedRepository.opprettUtenDuplikater(
                        abonnementer.map { it.event(Hendelsestype.OppdatertAdresse) },
                        10.seconds,
                    )
                }
            }

            "gi oppdatert adresse for kontaktadresse" {
                val abonnement = abonnement(fødselsnummer, skatteetaten)
                coEvery { abonnementRepository.hentAbonnementer(any()) } returns listOf(abonnement)
                val events = slot<List<FeedEvent.Incoming>>()
                coEvery { feedRepository.opprettUtenDuplikater(capture(events), any()) } just runs

                service.håndter(Livshendelse.Kontaktadresse(listOf(fødselsnummer)))

                events.captured shouldBe listOf(abonnement.event(Hendelsestype.OppdatertAdresse))
            }

            "ikke skrive til feeden når personen ikke har abonnement" {
                coEvery { abonnementRepository.hentAbonnementer(any()) } returns emptyList()

                service.håndter(Livshendelse.Bostedsadresse(listOf(fødselsnummer)))

                coVerify(exactly = 0) { feedRepository.opprettUtenDuplikater(any(), any()) }
            }
        }

        "adressebeskyttelse" should {
            listOf(
                Gradering.STRENGT_FORTROLIG_UTLAND to AdressebeskyttelseGradering.GRADERT,
                Gradering.STRENGT_FORTROLIG to AdressebeskyttelseGradering.GRADERT,
                Gradering.FORTROLIG to AdressebeskyttelseGradering.GRADERT,
                Gradering.UGRADERT to AdressebeskyttelseGradering.UGRADERT,
            ).forEach { (gradering, forventet) ->
                "gi $forventet for $gradering" {
                    val abonnement = abonnement(fødselsnummer, skatteetaten)
                    coEvery { abonnementRepository.hentAbonnementer(any()) } returns listOf(abonnement)
                    val events = slot<List<FeedEvent.Incoming>>()
                    coEvery { feedRepository.opprettUtenDuplikater(capture(events), any()) } just runs

                    service.håndter(Livshendelse.Adressebeskyttelse(listOf(fødselsnummer), gradering))

                    events.captured shouldBe listOf(abonnement.event(Hendelsestype.Adressebeskyttelse(forventet)))
                }
            }
        }
    })
