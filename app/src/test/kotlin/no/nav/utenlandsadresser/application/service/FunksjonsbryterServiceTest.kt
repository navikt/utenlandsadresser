package no.nav.utenlandsadresser.application.service

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import no.nav.utenlandsadresser.application.port.outbound.FeatureToggles
import no.nav.utenlandsadresser.application.port.outbound.Toggle

class FunksjonsbryterServiceTest :
    WordSpec({
        fun service(vararg påslått: Toggle) =
            FunksjonsbryterService(
                object : FeatureToggles {
                    override fun isEnabled(toggle: Toggle): Boolean = toggle in påslått
                },
            )

        "erTilgjengelig" should {
            "be true when UTENLANDSK_ID is enabled" {
                service(Toggle.UTENLANDSK_ID).erTilgjengelig() shouldBe true
            }

            "be false when UTENLANDSK_ID is disabled" {
                service().erTilgjengelig() shouldBe false
            }
        }
    })
