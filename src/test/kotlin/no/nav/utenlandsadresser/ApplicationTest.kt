package no.nav.utenlandsadresser

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import no.nav.utenlandsadresser.plugins.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""
                Puffet ris danser,
                Sprøtt i munnens glede, lett,
                Smaken av luftig.
            """.trimIndent(), bodyAsText())
        }
    }
}
