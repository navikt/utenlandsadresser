package no.nav.utenlandsadresser.domain

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

@JvmInline
value class Fødselsnummer private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String): Either<Error, Fødselsnummer> = either {
            ensure("""\d{11}""".toRegex().matches(value)) { Error.InvalidFormat }
            Fødselsnummer(value)
        }
    }

    sealed class Error {
        data object InvalidFormat : Error()
    }
}
