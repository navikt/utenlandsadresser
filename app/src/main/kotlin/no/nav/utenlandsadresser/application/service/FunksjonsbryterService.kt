package no.nav.utenlandsadresser.application.service

import no.nav.utenlandsadresser.application.port.inbound.UtenlandskIdTilgjengelig
import no.nav.utenlandsadresser.application.port.outbound.FeatureToggles
import no.nav.utenlandsadresser.application.port.outbound.Toggle

/** Svarer på om funksjonalitet bak en funksjonsbryter er tilgjengelig. */
class FunksjonsbryterService(
    private val featureToggles: FeatureToggles,
) : UtenlandskIdTilgjengelig {
    override fun erTilgjengelig(): Boolean = featureToggles.isEnabled(Toggle.UTENLANDSK_ID)
}
