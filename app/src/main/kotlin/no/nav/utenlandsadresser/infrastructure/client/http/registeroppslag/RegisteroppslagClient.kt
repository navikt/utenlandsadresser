package no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Postadresse

interface RegisteroppslagClient {
    suspend fun getPostadresse(identitetsnummer: Identitetsnummer): Either<GetPostadresseError, Postadresse>
}

sealed class GetPostadresseError {
    data object UgyldigForespørsel : GetPostadresseError()

    data object UkjentAdresse : GetPostadresseError()

    data object IngenTilgang : GetPostadresseError()

    data class UkjentFeil(
        val message: String,
    ) : GetPostadresseError()
}
