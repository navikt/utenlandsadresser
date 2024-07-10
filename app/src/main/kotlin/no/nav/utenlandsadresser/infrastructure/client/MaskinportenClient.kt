package no.nav.utenlandsadresser.infrastructure.client

interface MaskinportenClient {
    suspend fun getAccessToken(): String
}