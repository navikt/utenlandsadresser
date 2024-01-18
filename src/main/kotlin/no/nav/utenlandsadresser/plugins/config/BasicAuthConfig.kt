package no.nav.utenlandsadresser.plugins.config

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import no.nav.utenlandsadresser.domain.Password
import no.nav.utenlandsadresser.domain.Username

data class BasicAuthConfig private constructor(
    val username: Username,
    val password: Password,
) {

    companion object {
        operator fun invoke(username: String?, password: String?): EitherNel<Error, BasicAuthConfig> =
            either {
                zipOrAccumulate(
                    { ensureNotNull(username) { Error.NameMissing } },
                    { ensureNotNull(password) { Error.PasswordMissing } }
                ) { username, password -> BasicAuthConfig(Username(username), Password(password)) }
            }
    }

    sealed class Error {
        data object NameMissing : Error()
        data object PasswordMissing : Error()
    }
}