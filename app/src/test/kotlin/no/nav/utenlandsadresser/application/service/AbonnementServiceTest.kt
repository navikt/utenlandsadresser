package no.nav.utenlandsadresser.application.service

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.utenlandsadresser.FastClock
import no.nav.utenlandsadresser.application.port.inbound.StartAbonnementError
import no.nav.utenlandsadresser.application.port.inbound.StoppAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.AbonnementOppretter
import no.nav.utenlandsadresser.application.port.outbound.AbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.HentPostadresseError
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementMedEventError
import no.nav.utenlandsadresser.application.port.outbound.PostadresseOppslag
import no.nav.utenlandsadresser.application.port.outbound.SlettAbonnementError
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Land
import no.nav.utenlandsadresser.domain.Landkode
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import kotlin.time.Clock
import kotlin.uuid.Uuid

class AbonnementServiceTest :
    WordSpec({
        val abonnementRepository = mockk<AbonnementRepository>()
        val postadresseOppslag = mockk<PostadresseOppslag>()
        val abonnementOppretter = mockk<AbonnementOppretter>()
        val abonnementService = AbonnementService(abonnementRepository, postadresseOppslag, abonnementOppretter, FastClock())

        val identitetsnummer = Identitetsnummer("12345678910")
        val organisasjonsnummer = Organisasjonsnummer("123456789")

        val utenlandsk =
            Postadresse.Utenlandsk(
                adresselinje1 = null,
                adresselinje2 = null,
                adresselinje3 = null,
                postnummer = null,
                poststed = null,
                landkode = Landkode(value = "UK"),
                land = Land(value = "NOR"),
            )

        val abonnementId = Uuid.random()
        val abonnement =
            Abonnement(
                id = abonnementId,
                identitetsnummer = identitetsnummer,
                organisasjonsnummer = organisasjonsnummer,
                opprettet =
                    Clock.System
                        .now(),
            )

        "start abonnement" should {
            "return error when abonnement already exist" {
                coEvery { postadresseOppslag.hentPostadresse(any()) } returns utenlandsk.right()
                coEvery {
                    abonnementOppretter.opprettMedEvent(any(), any())
                } returns OpprettAbonnementMedEventError.AbonnementFinnesAllerede(abonnement).left()

                abonnementService.start(
                    identitetsnummer,
                    organisasjonsnummer,
                ) shouldBeEqual StartAbonnementError.AbonnementFinnesAllerede(abonnement).left()
            }

            "return error when failing to get postadresse" {
                coEvery { postadresseOppslag.hentPostadresse(any()) } returns HentPostadresseError.UgyldigForespørsel.left()

                abonnementService.start(
                    identitetsnummer,
                    organisasjonsnummer,
                ) shouldBeEqual StartAbonnementError.KunneIkkeHentePostadresse.left()
            }

            "return abonnement when abonnement is created" {
                coEvery { postadresseOppslag.hentPostadresse(any()) } returns utenlandsk.right()
                coEvery {
                    abonnementOppretter.opprettMedEvent(any(), any())
                } returns abonnement.right()

                abonnementService.start(
                    identitetsnummer,
                    organisasjonsnummer,
                ) shouldBeEqual abonnement.right()
            }

            "return abonnement when abonnement is created on falsk identiet" {
                coEvery { postadresseOppslag.hentPostadresse(any()) } returns HentPostadresseError.FalskIdentiet.left()
                coEvery { abonnementOppretter.opprettMedEvent(any(), any()) } returns abonnement.right()

                abonnementService.start(
                    identitetsnummer,
                    organisasjonsnummer,
                ) shouldBeEqual abonnement.right()
            }
        }

        "stop abonnement" should {
            "return error when abonnement is not found" {
                coEvery {
                    abonnementRepository.slettAbonnement(
                        abonnementId,
                        organisasjonsnummer,
                    )
                } returns SlettAbonnementError.IkkeFunnet.left()

                abonnementService.stopp(
                    abonnementId,
                    organisasjonsnummer,
                ) shouldBeEqual StoppAbonnementError.AbonnementIkkeFunnet.left()
            }

            "return unit when abonnement is stopped" {
                coEvery {
                    abonnementRepository.slettAbonnement(
                        abonnementId,
                        organisasjonsnummer,
                    )
                } returns Unit.right()

                abonnementService.stopp(abonnementId, organisasjonsnummer) shouldBeEqual Unit.right()
            }
        }
    })
