package no.nav.utenlandsadresser.infrastructure.client

import kotlinx.coroutines.CoroutineScope

interface LivshendelserConsumer {
    suspend fun CoroutineScope.consumeLivshendelser(topic: String)
}