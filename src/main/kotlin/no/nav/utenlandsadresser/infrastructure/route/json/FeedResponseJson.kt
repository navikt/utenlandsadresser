package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
data class FeedResponseJson(
    val abonnementId: String,
    val identitetsnummer: String,
    val utenlandskPostadresse: UtenlandskPostadresseJson,
) {
    companion object {
        fun fromDomain(feedEvent: FeedEvent.Outgoing, postadresse: Postadresse): FeedResponseJson = FeedResponseJson(
            identitetsnummer = feedEvent.identitetsnummer.value,
            abonnementId = feedEvent.abonnementId.toString(),
            utenlandskPostadresse = when (postadresse) {
                is Postadresse.Norsk,
                Postadresse.Empty -> UtenlandskPostadresseJson.empty()
                is Postadresse.Utenlandsk -> UtenlandskPostadresseJson.fromDomain(postadresse)
            },
        )
    }
}
