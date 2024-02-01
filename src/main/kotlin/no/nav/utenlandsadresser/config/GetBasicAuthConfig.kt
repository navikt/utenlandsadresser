package no.nav.utenlandsadresser.config

import arrow.core.getOrElse
import no.nav.utenlandsadresser.plugin.config.BasicAuthConfig
import org.slf4j.Logger

fun getDevApiBasicAuthConfig(logger: Logger): BasicAuthConfig? {
    return BasicAuthConfig(
        username = System.getenv("DEV_API_USERNAME"),
        password = System.getenv("DEV_API_PASSWORD"),
    ).getOrElse { errors ->
        errors.forEach {
            logger.error(it.toLogMessage())
        }
        null
    }
}

private fun BasicAuthConfig.Error.toLogMessage(): String = when (this) {
    BasicAuthConfig.Error.NameMissing -> "Environment variable DEV_API_USERNAME not set"
    BasicAuthConfig.Error.PasswordMissing -> "Environment variable DEV_API_PASSWORD not set"
}