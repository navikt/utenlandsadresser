package no.nav.utenlandsadresser.app

import kotlinx.coroutines.CoroutineScope

interface LivshendelserConsumer {
    suspend fun CoroutineScope.consumeLivshendelser(topic: String)
}
