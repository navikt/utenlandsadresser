package no.nav.utenlandsadresser.clients

interface MaskinportenClient {
    suspend fun getAccessToken(): String
}