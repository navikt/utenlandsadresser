package no.nav.utenlandsadresser.adapter.outbound.maskinporten

interface MaskinportenClient {
    suspend fun getAccessToken(): String
}
