package no.nav.utenlandsadresser.app

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

/**
 * Faste toggler, brukt lokalt og i tester.
 */
class StaticFeatureToggles(
    private val enabled: Set<Toggle>,
) : FeatureToggles {
    override fun isEnabled(toggle: Toggle): Boolean = toggle in enabled

    companion object {
        fun allEnabled(): StaticFeatureToggles = StaticFeatureToggles(Toggle.entries.toSet())
    }
}
