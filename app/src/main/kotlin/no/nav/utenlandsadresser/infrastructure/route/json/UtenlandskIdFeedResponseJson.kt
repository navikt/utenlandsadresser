package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable

@Serializable
data class UtenlandskIdResponseJson(
    val identitetsnummer: String,
    // ISO 3166-1 alpha-3 landskode
    val utstederland: String,
    val kilde: String,
)

@Serializable
data class UtenlandskIdFeedResponseJson(
    val abonnementId: String,
    val identitetsnummer: String,
    val utenlandskId: List<UtenlandskIdResponseJson>,
    val hendelsestype: UtenlandskIdHendelsestypeJson,
)
