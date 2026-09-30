package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import no.nav.utenlandsadresser.domain.UtenlandskIdHendelsestype

/**
 * Lagres som ordinal. Nye verdier må derfor legges til sist.
 */
enum class UtenlandskIdHendelsestypePostgres {
    OPPDATERT_UTENLANDSK_ID,
    ;

    companion object {
        fun fromDomain(hendelsestype: UtenlandskIdHendelsestype): UtenlandskIdHendelsestypePostgres =
            when (hendelsestype) {
                UtenlandskIdHendelsestype.OppdatertUtenlandskId -> OPPDATERT_UTENLANDSK_ID
            }
    }

    fun toDomain(): UtenlandskIdHendelsestype =
        when (this) {
            OPPDATERT_UTENLANDSK_ID -> UtenlandskIdHendelsestype.OppdatertUtenlandskId
        }
}
