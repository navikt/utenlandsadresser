package no.nav.utenlandsadresser.app

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.DeleteAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.InitAbonnementError
import org.slf4j.LoggerFactory
import java.util.*

class AbonnementService(
    private val abbonementRepository: AbonnementRepository,
    private val registeroppslagClient: RegisteroppslagClient,
    private val abonnementInitializer: AbonnementInitializer,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun startAbonnement(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StartAbonnementError, Abonnement> =
        either {
            val abonnement =
                Abonnement(
                    UUID.randomUUID(),
                    organisasjonsnummer = organisasjonsnummer,
                    identitetsnummer = identitetsnummer,
                    opprettet = Clock.System.now(),
                )

            val postadresse =
                registeroppslagClient
                    .getPostadresse(identitetsnummer)
                    .getOrElse {
                        when (it) {
                            GetPostadresseError.IngenTilgang,
                            GetPostadresseError.UgyldigForespørsel,
                            is GetPostadresseError.UkjentFeil,
                            -> {
                                logger.error("Failed to get postadresse: {}", it)
                                raise(StartAbonnementError.FailedToGetPostadresse)
                            }

                            GetPostadresseError.UkjentAdresse -> null
                        }
                    }

            return abonnementInitializer.initAbonnement(abonnement, postadresse).mapLeft {
                when (it) {
                    is InitAbonnementError.AbonnementAlreadyExists -> StartAbonnementError.AbonnementAlreadyExists(it.abonnement)
                }
            }
        }

    suspend fun stopAbonnement(
        abonnementId: UUID,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<StoppAbonnementError, Unit> =
        abbonementRepository.deleteAbonnement(abonnementId, organisasjonsnummer).mapLeft {
            when (it) {
                DeleteAbonnementError.NotFound -> StoppAbonnementError.AbonnementNotFound
            }
        }
}

sealed class StartAbonnementError {
    data class AbonnementAlreadyExists(
        val abonnement: Abonnement,
    ) : StartAbonnementError()

    data object FailedToGetPostadresse : StartAbonnementError()
}

sealed class StoppAbonnementError {
    data object AbonnementNotFound : StoppAbonnementError()
}
