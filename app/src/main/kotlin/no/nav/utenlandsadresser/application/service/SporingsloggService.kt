package no.nav.utenlandsadresser.application.service

import no.nav.utenlandsadresser.application.port.inbound.SkrivSporingslogg
import no.nav.utenlandsadresser.application.port.inbound.SlettSporingslogg
import no.nav.utenlandsadresser.application.port.outbound.SporingsloggRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import kotlin.time.Clock
import kotlin.time.Duration

class SporingsloggService(
    private val sporingsloggRepository: SporingsloggRepository,
    private val clock: Clock,
) : SkrivSporingslogg,
    SlettSporingslogg {
    override suspend fun skriv(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        json: String,
    ) {
        sporingsloggRepository.loggJson(
            identitetsnummer = identitetsnummer,
            organisasjonsnummer = organisasjonsnummer,
            json = json,
            tidspunktForUtlevering = clock.now(),
        )
    }

    override suspend fun slettEldreEnn(alder: Duration) {
        sporingsloggRepository.slettSporingsloggerFør(clock.now() - alder)
    }
}
