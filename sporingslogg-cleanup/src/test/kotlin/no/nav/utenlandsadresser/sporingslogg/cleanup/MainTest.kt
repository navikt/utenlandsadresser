package no.nav.utenlandsadresser.sporingslogg.cleanup

import com.sksamuel.hoplite.ConfigLoader
import io.kotest.core.annotation.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.infrastructure.client.http.createHttpClient
import no.nav.utenlandsadresser.sporingslogg.cleanup.config.SporingsloggCleanupConfig
import no.nav.utenlandsadresser.sporingslogg.cleanup.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.util.years

@DoNotParallelize
class MainTest : WordSpec() {
    init {
        val configLoader = mockk<ConfigLoader>()

        beforeTest {
            mockkObject(AppEnv)
            mockkObject(ConfigLoader)
            every { ConfigLoader.invoke() } returns configLoader
            mockkStatic(::createHttpClient)
        }

        afterTest {
            unmockkAll()
        }

        val sporingsloggCleanupConfig =
            SporingsloggCleanupConfig(
                UtenlandsadresserConfig(
                    baseUrl = "http://mocked_url",
                ),
            )

        "main" should {
            "send correct duration" {
                val expectedDuration = 10.years.toIsoString()
                val mockEngine =
                    MockEngine { request ->
                        request.method shouldBe HttpMethod.Delete
                        request.url.toString() shouldContain "olderThan=$expectedDuration"

                        respond(
                            content = "",
                            status = HttpStatusCode.OK,
                            headers = headersOf(),
                        )
                    }

                every { createHttpClient() } returns HttpClient(mockEngine)
                every { AppEnv.getFromEnvVariable("APP_ENV") } returns AppEnv.LOCAL
                every { configLoader.loadConfigOrThrow<SporingsloggCleanupConfig>(any<List<String>>(), any()) } returns
                    sporingsloggCleanupConfig

                main()
            }
        }
    }
}
