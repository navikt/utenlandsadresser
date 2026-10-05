package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.adapter.outbound.maskinporten.MaskinportenClient
import no.nav.utenlandsadresser.application.port.outbound.HentUtenlandskId
import no.nav.utenlandsadresser.application.port.outbound.RegisteroppslagClient

data class Clients(
    val regOppslagClient: RegisteroppslagClient,
    val maskinportenClient: MaskinportenClient,
    val hentUtenlandskIdClient: HentUtenlandskId,
)