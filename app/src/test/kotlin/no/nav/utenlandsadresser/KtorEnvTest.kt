package no.nav.utenlandsadresser

import io.kotest.core.spec.style.WordSpec
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.shouldBe

class KtorEnvTest : WordSpec({
    "fromEnvVariable" should {
        "return LOCAL when env does not exist" {
            AppEnv.getFromEnvVariable("APP_ENV") shouldBe AppEnv.LOCAL
        }
        "return LOCAL when env is empty" {
            withEnvironment("APP_ENV", "") {
                AppEnv.getFromEnvVariable("APP_ENV") shouldBe AppEnv.LOCAL
            }
        }
        "return LOCAL when env is something else" {
            withEnvironment("APP_ENV", "something else") {
                AppEnv.getFromEnvVariable("APP_ENV") shouldBe AppEnv.LOCAL
            }
        }
        "return DEV_GCP when env is dev-gcp" {
            withEnvironment("APP_ENV", "dev-gcp") {
                AppEnv.getFromEnvVariable("APP_ENV") shouldBe AppEnv.DEV_GCP
            }
        }
        "return LOCAL when env is local" {
            withEnvironment("APP_ENV", "local") {
                AppEnv.getFromEnvVariable("APP_ENV") shouldBe AppEnv.LOCAL
            }
        }
    }
})