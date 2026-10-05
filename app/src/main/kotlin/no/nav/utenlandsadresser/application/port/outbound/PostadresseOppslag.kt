package no.nav.utenlandsadresser.application.port.outbound

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Postadresse

interface PostadresseOppslag {
    suspend fun hentPostadresse(identitetsnummer: Identitetsnummer): Either<HentPostadresseError, Postadresse>
}

sealed class HentPostadresseError {
    data object UgyldigForespørsel : HentPostadresseError()

    data object UkjentAdresse : HentPostadresseError()

    data object IngenTilgang : HentPostadresseError()

    data object FalskIdentiet : HentPostadresseError()

    data class UkjentFeil(
        val message: String,
    ) : HentPostadresseError()
}
