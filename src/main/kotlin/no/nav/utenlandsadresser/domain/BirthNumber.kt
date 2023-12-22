package no.nav.utenlandsadresser.domain

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

@JvmInline
value class BirthNumber private constructor(val value: String) {
    companion object {
        fun create(value: String): Either<BirthNumberError, BirthNumber> = either {
            ensure("""\d{11}""".toRegex().matches(value)) { BirthNumberError.InvalidFormat }
            BirthNumber(value)
        }
    }
}

sealed class BirthNumberError {
    data object InvalidFormat : BirthNumberError()
}