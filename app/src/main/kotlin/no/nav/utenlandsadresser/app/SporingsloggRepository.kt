package no.nav.utenlandsadresser.app

import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

interface SporingsloggRepository {
    suspend fun loggPostadresse(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        postadresse: Postadresse.Utenlandsk,
        tidspunktForUtlevering: Instant = Clock.System.now(),
    )

    suspend fun loggJson(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        json: JsonElement,
        tidspunktForUtlevering: Instant = Clock.System.now(),
    )

    suspend fun deleteSporingsloggerOlderThan(duration: Duration)
}
