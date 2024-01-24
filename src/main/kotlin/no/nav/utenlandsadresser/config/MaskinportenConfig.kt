package no.nav.utenlandsadresser.config

data class MaskinportenConfig(
    val clientId: String,
    val clientJwk: String,
    val scopes: String,
    val wellKnownUrl: String,
    val issuer: String,
    val tokenEndpoint: String,
) {
    companion object {
        fun getFromEnv(): MaskinportenConfig = MaskinportenConfig(
            clientId = System.getenv("MASKINPORTEN_CLIENT_ID"),
            clientJwk = System.getenv("MASKINPORTEN_CLIENT_JWK"),
            scopes = System.getenv("MASKINPORTEN_SCOPES"),
            wellKnownUrl = System.getenv("MASKINPORTEN_WELL_KNOWN_URL"),
            issuer = System.getenv("MASKINPORTEN_ISSUER"),
            tokenEndpoint = System.getenv("MASKINPORTEN_TOKEN_ENDPOINT"),
        )
    }
}