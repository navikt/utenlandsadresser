package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
data class PostadresseFeedResponseJson(
    val abonnementId: String,
    val identitetsnummer: String,
    val utenlandskPostadresse: UtenlandskPostadresseJson?,
    val hendelsestype: PostadresseHendelsestypeJson,
) {
    companion object {
        fun fromDomain(
            feedEvent: FeedEvent.Outgoing,
            postadresse: Postadresse.Utenlandsk?,
        ): PostadresseFeedResponseJson =
            PostadresseFeedResponseJson(
                identitetsnummer = feedEvent.identitetsnummer.value,
                abonnementId = feedEvent.abonnementId.toString(),
                utenlandskPostadresse =
                    if (feedEvent.hendelsestype.erGradert) {
                        null
                    } else {
                        postadresse?.let { UtenlandskPostadresseJson.fromDomain(it) }
                    },
                hendelsestype = PostadresseHendelsestypeJson.fromDomain(feedEvent.hendelsestype),
            )
    }
}

