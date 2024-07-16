package no.nav.utenlandsadresser.infrastructure.kafka

import kotlinx.coroutines.CoroutineScope

interface LivshendelserConsumer {
    suspend fun CoroutineScope.consumeLivshendelser(topic: String)
}
