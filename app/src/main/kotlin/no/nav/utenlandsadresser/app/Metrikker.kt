package no.nav.utenlandsadresser.app

/**
 * Forretningsmetrikker. Påvirker ikke hva tjenestene gjør, og sendes derfor inn som context parameter
 * der de brukes, ikke i konstruktøren.
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
