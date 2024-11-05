package no.nav.utenlandsadresser.hent.utenlandsadresser.config

data class PdlMottakConfig(
    val baseUrl: String,
    val namespace: String,
    val applicationName: String,
    val cluster: String,
    val scope: String,
)
