package no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json

import kotlinx.serialization.Serializable

@Serializable
data class PersonopplysningJson(
    val endringstype: EndringstypeJson,
    val ident: String,
    val opplysningstype: OpplysningstypeJson,
    val endringsmelding: EndringsmeldingJson,
    val opplysningsId: String?,
)
