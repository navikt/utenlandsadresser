package no.nav.utenlandsadresser.plugins

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

fun withHttpClient(block: (HttpClient) -> Unit) {
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }.use(block)
}
