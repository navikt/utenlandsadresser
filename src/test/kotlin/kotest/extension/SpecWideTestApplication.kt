package kotest.extension

import io.kotest.core.spec.DslDrivenSpec
import io.ktor.server.testing.*

fun DslDrivenSpec.specWideTestApplication(
    testApplicationBuilder: TestApplicationBuilder.() -> Unit
): TestApplication = TestApplication(testApplicationBuilder).also { testApplication ->
    afterSpec { testApplication.stop() }
}
