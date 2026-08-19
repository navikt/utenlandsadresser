package no.nav.utenlandsadresser.kotest.extension

import com.github.tomakehurst.wiremock.WireMockServer
import io.kotest.core.spec.AbstractSpec
import io.kotest.extensions.wiremock.WireMockListener

fun AbstractSpec.setupWiremockServer(): WireMockServer {
    val mockServer = WireMockServer(0)
    extension(WireMockListener.perSpec(mockServer))

    afterTest {
        mockServer.resetAll()
    }

    return mockServer
}
