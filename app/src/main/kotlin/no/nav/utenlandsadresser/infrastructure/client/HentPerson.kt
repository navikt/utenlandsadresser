package no.nav.utenlandsadresser.infrastructure.client

import arrow.core.Either
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet

interface HentUtenlandskId {
    suspend fun hentUtenlandskIdentitet(identitetsnummer: Identitetsnummer): Either<Error, List<UtenlandskIdentitet>>

    sealed class Error {
        data object Kommunikasjonsfeil : Error()

        data object FeilIRespons : Error()

        data object UgyldigUtstederland : Error()

        data object ManglerKildeForUtenlandskIdentitet : Error()
    }
}
