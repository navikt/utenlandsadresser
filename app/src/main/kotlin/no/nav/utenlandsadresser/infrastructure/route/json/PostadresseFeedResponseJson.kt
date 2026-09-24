package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
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
            when (feedEvent.hendelsestype) {
                is Hendelsestype.Adressebeskyttelse -> {
                    PostadresseFeedResponseJson(
                        identitetsnummer = feedEvent.identitetsnummer.value,
                        abonnementId = feedEvent.abonnementId.toString(),
                        utenlandskPostadresse = null,
                        hendelsestype = PostadresseHendelsestypeJson.fromDomain(feedEvent.hendelsestype),
                    )
                }

                Hendelsestype.OppdatertAdresse -> {
                    PostadresseFeedResponseJson(
                        identitetsnummer = feedEvent.identitetsnummer.value,
                        abonnementId = feedEvent.abonnementId.toString(),
                        utenlandskPostadresse = postadresse?.let { UtenlandskPostadresseJson.fromDomain(postadresse) },
                        hendelsestype = PostadresseHendelsestypeJson.fromDomain(feedEvent.hendelsestype),
                    )
                }
            }
    }
}

