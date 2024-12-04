package no.nav.utenlandsadresser.setup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.utenlandsadresser.EventConsumers

fun CoroutineScope.launchBackgroundJobs(eventConsumers: EventConsumers) {
    launch(Dispatchers.IO) {
        eventConsumers.livshendelserConsumer.use { consumer ->
            while (isActive) {
                consumer.consumeLivshendelser()
            }
        }
    }
}
