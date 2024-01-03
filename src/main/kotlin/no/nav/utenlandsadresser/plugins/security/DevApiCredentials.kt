package no.nav.utenlandsadresser.plugins.security

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate

data class DevApiCredentials private constructor(
    val name: String,
    val password: String,
) {
    companion object {
        operator fun invoke(name: String?, password: String?): EitherNel<Error, DevApiCredentials> =
            either {
                zipOrAccumulate(
                    { ensure(!name.isNullOrBlank()) { Error.NameMissing } },
                    { ensure(!password.isNullOrBlank()) { Error.PasswordMissing } }
                ) { _, _ -> DevApiCredentials(name!!, password!!) }
            }
    }

    sealed class Error {
        data object NameMissing : Error()
        data object PasswordMissing : Error()
    }
}