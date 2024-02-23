package no.nav.utenlandsadresser.infrastructure.client

import kotlinx.coroutines.CoroutineScope

interface LivshendelserConsumer {
    fun CoroutineScope.consumeLivshendelser()
}