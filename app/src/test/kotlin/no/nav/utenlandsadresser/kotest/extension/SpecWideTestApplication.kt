package no.nav.utenlandsadresser.kotest.extension

import io.kotest.core.spec.AbstractSpec
import io.ktor.server.testing.TestApplication
import io.ktor.server.testing.TestApplicationBuilder

fun AbstractSpec.specWideTestApplication(testApplicationBuilder: TestApplicationBuilder.() -> Unit): TestApplication =
    TestApplication(testApplicationBuilder).also { testApplication ->
        afterSpec { testApplication.stop() }
    }
