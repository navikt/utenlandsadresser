package no.nav.utenlandsadresser.infrastructure.client

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.domain.Fødselsnummer

interface RegisteroppslagClient {
    suspend fun getPostadresse(fødselsnummer: Fødselsnummer): Either<Error, Postadresse>

    sealed class Error {
        data object UgyldigForespørsel : Error()
        data object UkjentAdresse : Error()
        data object IngenTilgang : Error()
        data class Ukjent(val message: String) : Error()
    }
}