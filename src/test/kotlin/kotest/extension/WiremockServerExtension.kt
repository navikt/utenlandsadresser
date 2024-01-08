package kotest.extension

import com.github.tomakehurst.wiremock.WireMockServer
import io.kotest.core.spec.DslDrivenSpec
import io.kotest.extensions.wiremock.WireMockListener

fun DslDrivenSpec.setupWiremockServer(): WireMockServer {
    val mockServer = WireMockServer(0)
    register(WireMockListener.perSpec(mockServer))

    afterTest {
        mockServer.resetAll()
    }

    return mockServer
}
