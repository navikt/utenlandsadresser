package no.nav.utenlandsadresser.config

data class RegisteroppslagConfig(
    val baseUrl: String,
    val namespace: String,
    val applicationName: String,
    val cluster: String,
)