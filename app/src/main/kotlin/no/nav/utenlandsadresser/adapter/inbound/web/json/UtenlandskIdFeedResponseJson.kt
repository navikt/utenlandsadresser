package no.nav.utenlandsadresser.adapter.inbound.web.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet

@Serializable
data class UtenlandskIdResponseJson(
    val identitetsnummer: String,
    // ISO 3166-1 alpha-3 landskode
    val utstederland: String,
    val kilde: String,
) {
    companion object {
        fun fromDomain(utenlandskIdentitet: UtenlandskIdentitet): UtenlandskIdResponseJson =
            UtenlandskIdResponseJson(
                identitetsnummer = utenlandskIdentitet.identitetsnummer.identitetsnummer,
                utstederland = utenlandskIdentitet.utstederland.value,
                kilde = utenlandskIdentitet.kilde.kilde,
            )
    }
}

@Serializable
data class UtenlandskIdFeedResponseJson(
    val abonnementId: String,
    val identitetsnummer: String,
    val utenlandskId: List<UtenlandskIdResponseJson>,
    val hendelsestype: UtenlandskIdHendelsestypeJson,
) {
    companion object {
        fun fromDomain(
            feedEvent: UtenlandskIdFeedEvent.Outgoing,
            utenlandskeIdentiteter: List<UtenlandskIdentitet>,
        ): UtenlandskIdFeedResponseJson =
            UtenlandskIdFeedResponseJson(
                abonnementId = feedEvent.abonnementId.toString(),
                identitetsnummer = feedEvent.identitetsnummer.value,
                utenlandskId = utenlandskeIdentiteter.map(UtenlandskIdResponseJson::fromDomain),
                hendelsestype = UtenlandskIdHendelsestypeJson.fromDomain(feedEvent.hendelsestype),
            )
    }
}
