package no.nav.utenlandsadresser.app

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import kotlin.time.Duration

interface SporingsloggRepository {
    suspend fun loggPostadresse(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        postadresse: Postadresse.Utenlandsk,
        tidspunktForUtlevering: Instant = Clock.System.now(),
    )

    suspend fun deleteSporingsloggerOlderThan(duration: Duration)
}
