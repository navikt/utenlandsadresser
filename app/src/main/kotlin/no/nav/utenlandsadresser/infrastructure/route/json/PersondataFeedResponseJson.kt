package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable

/**
 * POC: Hendelsestype for persondata-feeden (v2-poc), hvor utenlandsk id er lagt til som
 * ekstra data i samme feed som postadresse. Dette er en egen enum for POC-en, og er ikke i bruk
 * av den eksisterende v1-APIen for postadresse ([PostadresseHendelsestypeJson]).
 */
enum class PersondataHendelsestypeJson {
    OPPDATERT_ADRESSE,
    SLETTET_ADRESSE,
    OPPDATERT_UTENLANDSK_ID,
}

@Serializable
data class PersondataFeedResponseJson(
    val abonnementId: String,
    val identitetsnummer: String,
    val utenlandskPostadresse: UtenlandskPostadresseJson?,
    val utenlandskId: List<UtenlandskIdResponseJson>,
    val hendelsestype: PersondataHendelsestypeJson,
)
