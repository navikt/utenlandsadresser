package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.adapter.outbound.maskinporten.MaskinportenClient
import no.nav.utenlandsadresser.application.port.outbound.PostadresseOppslag
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdOppslag

data class Clients(
    val postadresseOppslag: PostadresseOppslag,
    val maskinportenClient: MaskinportenClient,
    val utenlandskIdOppslag: UtenlandskIdOppslag,
)