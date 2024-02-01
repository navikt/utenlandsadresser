package no.nav.utenlandsadresser.infrastructure.client.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.BearerAuthPlugin
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig

fun configureHttpClient(): HttpClient = HttpClient(CIO) {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

fun configureAuthHttpClient(
    oAuthConfig: OAuthConfig,
    tokenClient: HttpClient = configureHttpClient(),
): HttpClient = HttpClient(CIO) {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }

    install(BearerAuthPlugin) {
        this.oAuthConfig = oAuthConfig
        this.tokenClient = tokenClient
    }
}
