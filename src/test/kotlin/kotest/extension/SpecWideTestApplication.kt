package kotest.extension

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.spec.Spec
import io.ktor.server.testing.*

class SpecWideTestApplication(
    testApplicationBuilder: TestApplicationBuilder.() -> Unit
) : AfterSpecListener {
    private val testApplication: TestApplication = TestApplication(testApplicationBuilder)

    val client = testApplication.client

    override suspend fun afterSpec(spec: Spec) {
        testApplication.stop()
    }
}
