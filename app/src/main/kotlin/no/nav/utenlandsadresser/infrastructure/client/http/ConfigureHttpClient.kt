package no.nav.utenlandsadresser.infrastructure.client.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.BearerAuthPlugin
import no.nav.utenlandsadresser.infrastructure.client.http.plugin.config.OAuthConfig

fun createHttpClient(): HttpClient =
    HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
    }

fun createAuthHttpClient(
    oAuthConfig: OAuthConfig,
    scopes: List<Scope>,
    tokenClient: HttpClient = createHttpClient(),
): HttpClient =
    HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }

        install(BearerAuthPlugin) {
            this.oAuthConfig = oAuthConfig
            this.tokenClient = tokenClient
            this.scopes = scopes
        }
    }
