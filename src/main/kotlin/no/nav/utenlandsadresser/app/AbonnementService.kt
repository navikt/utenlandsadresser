package no.nav.utenlandsadresser.app

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.client.GetPostadresseError
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementInitializer
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.DeleteAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.InitAbonnementError
import java.util.*

class AbonnementService(
    private val abbonementRepository: AbonnementRepository,
    private val registeroppslagClient: RegisteroppslagClient,
    private val abonnementInitializer: AbonnementInitializer
) {
    suspend fun startAbonnement(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer
    ): Either<StartAbonnementError, Abonnement> = either {
        val abonnement = Abonnement(
            UUID.randomUUID(),
            organisasjonsnummer = organisasjonsnummer,
            identitetsnummer = identitetsnummer,
            opprettet = Clock.System.now()
        )

        val postadresse = registeroppslagClient.getPostadresse(identitetsnummer)
            .getOrElse {
                when (it) {
                    GetPostadresseError.IngenTilgang,
                    GetPostadresseError.UgyldigForespørsel,
                    is GetPostadresseError.UkjentFeil,
                    GetPostadresseError.UkjentAdresse -> raise(StartAbonnementError.FailedToGetPostadresse)
                }
            }

        return abonnementInitializer.initAbonnement(abonnement, postadresse).mapLeft {
            when (it) {
                is InitAbonnementError.AbonnementAlreadyExists -> StartAbonnementError.AbonnementAlreadyExists(it.abonnement)
            }
        }
    }

    suspend fun stopAbonnement(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer
    ): Either<StoppAbonnementError, Unit> {
        return abbonementRepository.deleteAbonnement(identitetsnummer, organisasjonsnummer).mapLeft {
            when (it) {
                DeleteAbonnementError.NotFound -> StoppAbonnementError.AbonnementNotFound
            }
        }
    }

    suspend fun hentAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> {
        return abbonementRepository.getAbonnementer(identitetsnummer)
    }
}

sealed class StartAbonnementError {
    data class AbonnementAlreadyExists(val abonnement: Abonnement) : StartAbonnementError()
    data object FailedToGetPostadresse : StartAbonnementError()
}

sealed class StoppAbonnementError {
    data object AbonnementNotFound : StoppAbonnementError()
}
