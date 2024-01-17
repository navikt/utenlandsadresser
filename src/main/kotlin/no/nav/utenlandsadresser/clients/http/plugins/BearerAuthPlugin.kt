package no.nav.utenlandsadresser.clients.http.plugins

import arrow.core.getOrElse
import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.http.*
import no.nav.utenlandsadresser.domain.BearerToken
import no.nav.utenlandsadresser.clients.http.FetchTokenError
import no.nav.utenlandsadresser.clients.http.fetchToken
import no.nav.utenlandsadresser.plugins.config.OAuthConfig
import org.slf4j.LoggerFactory
import java.time.Instant

val BearerAuthPlugin = createClientPlugin("BearerAuth", ::BearerAuthConfig) {
    var bearerToken: BearerToken? = null
    var tokenExpiryTime = Instant.MIN
    val logger = LoggerFactory.getLogger("BearerAuth")
    val oAuthConfig = pluginConfig.oAuthConfig!!
    val tokenClient = pluginConfig.tokenClient!!

    onRequest { request, _ ->
        if (bearerToken == null || Instant.now().isAfter(tokenExpiryTime)) {
            val tokenInfo = fetchToken(tokenClient, oAuthConfig)
                .getOrElse { error ->
                    when (error) {
                        FetchTokenError.NoMatchingJsonFound -> logger.error("Unable to fetch token: $error")
                        is FetchTokenError.HttpError -> logger.error("Unable to fetch token: ${error.statusCode} ${error.body}")
                    }
                    return@onRequest
                }

            tokenExpiryTime = Instant.now().plusSeconds(tokenInfo.expiresIn.toLong())
            bearerToken = BearerToken(tokenInfo.accessToken)
        }

        bearerToken?.let {
            request.headers.append(HttpHeaders.Authorization, "Bearer ${it.value}")
        }
    }
}

class BearerAuthConfig {
    var oAuthConfig: OAuthConfig? = null
    var tokenClient: HttpClient? = null
}