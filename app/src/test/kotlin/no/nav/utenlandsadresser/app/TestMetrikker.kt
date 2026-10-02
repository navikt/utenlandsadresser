package no.nav.utenlandsadresser.app

/** Teller kallene i minnet, så testene kan sjekke hva som ble talt. */
class TestMetrikker : Metrikker {
    val utlevert = mutableMapOf<Feed, Int>()
    val stoppetAbonnementLest = mutableMapOf<Feed, Int>()

    override fun utlevert(feed: Feed) {
        utlevert.merge(feed, 1, Int::plus)
    }

    override fun stoppetAbonnementLest(feed: Feed) {
        stoppetAbonnementLest.merge(feed, 1, Int::plus)
    }

    fun nullstill() {
        utlevert.clear()
        stoppetAbonnementLest.clear()
    }
}

/** For tester som ikke bryr seg om metrikker. */
object NoopMetrikker : Metrikker {
    override fun utlevert(feed: Feed) = Unit

    override fun stoppetAbonnementLest(feed: Feed) = Unit
}
