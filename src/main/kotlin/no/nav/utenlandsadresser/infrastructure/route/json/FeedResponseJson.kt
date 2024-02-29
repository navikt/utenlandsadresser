package no.nav.utenlandsadresser.infrastructure.route.json

import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Postadresse

@Serializable
data class FeedResponseJson(
    val identitetsnummer: String,
    val utenlandskPostadresse: UtenlandskPostadresseJson,
) {
    companion object {
        fun fromDomain(identitetsnummer: Identitetsnummer, postadresse: Postadresse): FeedResponseJson = FeedResponseJson(
            identitetsnummer = identitetsnummer.value,
            utenlandskPostadresse = when (postadresse) {
                is Postadresse.Norsk,
                Postadresse.Empty -> UtenlandskPostadresseJson.empty()
                is Postadresse.Utenlandsk -> UtenlandskPostadresseJson.fromDomain(postadresse)
            },
        )
    }
}
