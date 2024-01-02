package no.nav.utenlandsadresser

import io.kotest.core.spec.style.WordSpec
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.shouldBe

class KtorEnvTest : WordSpec({
    "fromEnvVariable" should {
        "return LOCAL when env does not exist" {
            KtorEnv.getFromEnvVariable("KTOR_ENV") shouldBe KtorEnv.LOCAL
        }
        "return LOCAL when env is empty" {
            withEnvironment("KTOR_ENV", "") {
                KtorEnv.getFromEnvVariable("KTOR_ENV") shouldBe KtorEnv.LOCAL
            }
        }
        "return LOCAL when env is something else" {
            withEnvironment("KTOR_ENV", "something else") {
                KtorEnv.getFromEnvVariable("KTOR_ENV") shouldBe KtorEnv.LOCAL
            }
        }
        "return DEV_GCP when env is dev-gcp" {
            withEnvironment("KTOR_ENV", "dev-gcp") {
                KtorEnv.getFromEnvVariable("KTOR_ENV") shouldBe KtorEnv.DEV_GCP
            }
        }
        "return PROD_GCP when env is prod-gcp" {
            withEnvironment("KTOR_ENV", "prod-gcp") {
                KtorEnv.getFromEnvVariable("KTOR_ENV") shouldBe KtorEnv.PROD_GCP
            }
        }
    }
})