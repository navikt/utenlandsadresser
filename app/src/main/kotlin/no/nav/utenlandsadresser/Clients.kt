package no.nav.utenlandsadresser

import no.nav.utenlandsadresser.infrastructure.client.MaskinportenClient
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient

data class Clients(
    val regOppslagClient: RegisteroppslagClient,
    val maskinportenClient: MaskinportenClient,
)