package no.nav.utenlandsadresser.setup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.utenlandsadresser.EventConsumers
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig

fun CoroutineScope.launchBackgroundJobs(
    eventConsumers: EventConsumers,
    config: UtenlandsadresserConfig,
) {
    launch(Dispatchers.IO) {
        with(eventConsumers.livshendelserConsumer) {
            use {
                while (isActive) {
                    consumeLivshendelser(config.kafka.topic)
                }
            }
        }
    }
}
