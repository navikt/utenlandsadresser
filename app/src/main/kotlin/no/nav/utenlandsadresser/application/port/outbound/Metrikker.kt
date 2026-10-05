package no.nav.utenlandsadresser.application.port.outbound

/**
 * Forretningsmetrikker. Påvirker ikke hva tjenestene gjør.
 *
 * Tjenestene får dem i konstruktøren, så de inngående adapterne ikke trenger å kjenne til metrikker.
 */
interface Metrikker {
    /** Data er utlevert fra feeden og sporingslogget. */
    fun utlevert(feed: Feed)

    /** En hendelse er lest for et abonnement som er stoppet, og levert uten data. */
    fun stoppetAbonnementLest(feed: Feed)
}

enum class Feed {
    POSTADRESSE,
    UTENLANDSK_ID,
}
