package no.nav.utenlandsadresser.adapter.outbound.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.utenlandsadresser.application.port.outbound.Feed
import no.nav.utenlandsadresser.application.port.outbound.Metrikker

class MicrometerMetrikker(
    meterRegistry: MeterRegistry,
) : Metrikker {
    private val utlevert: Map<Feed, Counter> =
        Feed.entries.associateWith {
            meterRegistry.counter("utenlandsadresser.feed.utlevert", "feed", it.tag)
        }

    private val stoppetAbonnementLest: Map<Feed, Counter> =
        Feed.entries.associateWith {
            meterRegistry.counter("utenlandsadresser_feed_stoppet_abonnement_total", "feed", it.tag)
        }

    override fun utlevert(feed: Feed) {
        utlevert.getValue(feed).increment()
    }

    override fun stoppetAbonnementLest(feed: Feed) {
        stoppetAbonnementLest.getValue(feed).increment()
    }

    private val Feed.tag: String
        get() =
            when (this) {
                Feed.POSTADRESSE -> "postadresse"
                Feed.UTENLANDSK_ID -> "utenlandskid"
            }
}
