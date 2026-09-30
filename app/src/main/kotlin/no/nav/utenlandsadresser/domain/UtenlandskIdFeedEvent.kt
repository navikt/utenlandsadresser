package no.nav.utenlandsadresser.domain

import kotlin.uuid.Uuid

sealed class UtenlandskIdFeedEvent {
    abstract val identitetsnummer: Identitetsnummer
    abstract val abonnementId: Uuid
    abstract val hendelsestype: UtenlandskIdHendelsestype

    data class Incoming(
        override val identitetsnummer: Identitetsnummer,
        override val abonnementId: Uuid,
        override val hendelsestype: UtenlandskIdHendelsestype,
        val organisasjonsnummer: Organisasjonsnummer,
    ) : UtenlandskIdFeedEvent()

    data class Outgoing(
        override val identitetsnummer: Identitetsnummer,
        override val abonnementId: Uuid,
        override val hendelsestype: UtenlandskIdHendelsestype,
    ) : UtenlandskIdFeedEvent()
}
