package no.nav.utenlandsadresser.infrastructure.route.identitet.json

import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype

enum class UtenlandskIdHendelsestypeJson {
    OPPDATERT_UTENLANDSK_ID,
    ;

    companion object {
        fun fromDomain(hendelsestype: UtenlandskIdHendelsestype): UtenlandskIdHendelsestypeJson =
            when (hendelsestype) {
                UtenlandskIdHendelsestype.OppdatertUtenlandskId -> OPPDATERT_UTENLANDSK_ID
            }
    }
}
