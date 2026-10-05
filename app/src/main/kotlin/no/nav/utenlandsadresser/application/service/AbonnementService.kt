package no.nav.utenlandsadresser.application.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import no.nav.utenlandsadresser.application.port.inbound.StartAbonnement
import no.nav.utenlandsadresser.application.port.inbound.StartAbonnementError
import no.nav.utenlandsadresser.application.port.inbound.StoppAbonnement
import no.nav.utenlandsadresser.application.port.inbound.StoppAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.AbonnementOppretter
import no.nav.utenlandsadresser.application.port.outbound.AbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.HentPostadresseError
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementMedEventError
import no.nav.utenlandsadresser.application.port.outbound.PostadresseOppslag
import no.nav.utenlandsadresser.application.port.outbound.SlettAbonnementError
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.Uuid

class AbonnementService(
    private val abbonementRepository: AbonnementRepository,
    private val postadresseOppslag: PostadresseOppslag,
    private val abonnementOppretter: AbonnementOppretter,
    private val clock: Clock,
) : StartAbonnement,
    StoppAbonnement {
    private val logger = LoggerFactory.getLogger(AbonnementService::class.java)

    override suspend fun start(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StartAbonnementError, Abonnement> =
        either {
            val abonnement =
                Abonnement(
                    Uuid.random(),
                    organisasjonsnummer = organisasjonsnummer,
                    identitetsnummer = identitetsnummer,
                    opprettet = clock.now(),
                )

            val postadresse =
                postadresseOppslag
                    .hentPostadresse(identitetsnummer)
                    .getOrElse {
                        when (it) {
                            HentPostadresseError.IngenTilgang,
                            HentPostadresseError.UgyldigForespørsel,
                            is HentPostadresseError.UkjentFeil,
                            -> {
                                logger.error("Failed to get postadresse: {}", it)
                                raise(StartAbonnementError.KunneIkkeHentePostadresse)
                            }

                            HentPostadresseError.UkjentAdresse,
                            HentPostadresseError.FalskIdentiet,
                            -> {
                                null
                            }
                        }
                    }

            return abonnementOppretter.opprettMedEvent(abonnement, postadresse).mapLeft {
                when (it) {
                    is OpprettAbonnementMedEventError.AbonnementFinnesAllerede -> StartAbonnementError.AbonnementFinnesAllerede(it.abonnement)
                }
            }
        }

    override suspend fun stopp(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StoppAbonnementError, Unit> =
        abbonementRepository.slettAbonnement(abonnementId, organisasjonsnummer).mapLeft {
            when (it) {
                SlettAbonnementError.IkkeFunnet -> StoppAbonnementError.AbonnementIkkeFunnet
            }
        }
}
