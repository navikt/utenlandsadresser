package no.nav.utenlandsadresser.domain

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

@JvmInline
value class BaseUrl private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String): Either<Error, BaseUrl> = either {
            ensure("""https?://.*""".toRegex().matches(value)) { Error.InvalidFormat(value) }
            BaseUrl(value)
        }
    }

    sealed class Error {
        data class InvalidFormat(val baseUrl: String) : Error()
    }
}