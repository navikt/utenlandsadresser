package no.nav.utenlandsadresser.infrastructure.metrics

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.string.shouldContain
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.utenlandsadresser.app.Feed

/**
 * Låser navnene og taggene slik Prometheus ser dem. Dashboards og alerts bruker disse.
 */
class MicrometerMetrikkerTest :
    WordSpec({
        "MicrometerMetrikker" should {
            "count utlevert per feed" {
                val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                val metrikker = MicrometerMetrikker(registry)

                metrikker.utlevert(Feed.POSTADRESSE)
                metrikker.utlevert(Feed.UTENLANDSK_ID)
                metrikker.utlevert(Feed.UTENLANDSK_ID)

                val scrape = registry.scrape()
                scrape shouldContain """utenlandsadresser_feed_utlevert_total{feed="postadresse"} 1.0"""
                scrape shouldContain """utenlandsadresser_feed_utlevert_total{feed="utenlandskid"} 2.0"""
            }

            "count stoppet abonnement lest per feed" {
                val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                val metrikker = MicrometerMetrikker(registry)

                metrikker.stoppetAbonnementLest(Feed.POSTADRESSE)
                metrikker.stoppetAbonnementLest(Feed.UTENLANDSK_ID)
                metrikker.stoppetAbonnementLest(Feed.UTENLANDSK_ID)

                val scrape = registry.scrape()
                scrape shouldContain """utenlandsadresser_feed_stoppet_abonnement_total{feed="postadresse"} 1.0"""
                scrape shouldContain """utenlandsadresser_feed_stoppet_abonnement_total{feed="utenlandskid"} 2.0"""
            }
        }
    })
