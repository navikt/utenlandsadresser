package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
data class FeedResponseJson(
    val abonnementId: String,
    val identitetsnummer: String,
    val utenlandskPostadresse: UtenlandskPostadresseJson?,
    val hendelsestype: HendelsestypeJson,
) {
    companion object {
        fun fromDomain(feedEvent: FeedEvent.Outgoing, postadresse: Postadresse.Utenlandsk?): FeedResponseJson =
            when (feedEvent.hendelsestype) {
                is Hendelsestype.Adressebeskyttelse -> FeedResponseJson(
                    identitetsnummer = feedEvent.identitetsnummer.value,
                    abonnementId = feedEvent.abonnementId.toString(),
                    utenlandskPostadresse = null,
                    hendelsestype = HendelsestypeJson.fromDomain(feedEvent.hendelsestype),
                )

                Hendelsestype.OppdatertAdresse -> FeedResponseJson(
                    identitetsnummer = feedEvent.identitetsnummer.value,
                    abonnementId = feedEvent.abonnementId.toString(),
                    utenlandskPostadresse = postadresse?.let { UtenlandskPostadresseJson.fromDomain(postadresse) },
                    hendelsestype = HendelsestypeJson.fromDomain(feedEvent.hendelsestype),
                )
            }
    }
}
