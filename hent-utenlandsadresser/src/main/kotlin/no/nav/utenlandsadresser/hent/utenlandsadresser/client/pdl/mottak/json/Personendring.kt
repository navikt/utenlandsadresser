package no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json

import kotlinx.serialization.Serializable

@Serializable
data class PersonendringJson(
    val personopplysninger: List<PersonopplysningJson>,
)
