package no.nav.utenlandsadresser.application.port.outbound

/**
 * Feature toggles som styrer hvilken funksjonalitet som er tilgjengelig.
 *
 * Implementasjonen skal svare `false` når den ikke vet, for eksempel når Unleash ikke kan nås.
 */
interface FeatureToggles {
    fun isEnabled(toggle: Toggle): Boolean
}

enum class Toggle(
    val navn: String,
) {
    /** All funksjonalitet under `/api/v1/utenlandskid`. Fjernes når utenlandsk id er i prod. */
    UTENLANDSK_ID("utenlandsadresser.utenlandsk-id"),
}
