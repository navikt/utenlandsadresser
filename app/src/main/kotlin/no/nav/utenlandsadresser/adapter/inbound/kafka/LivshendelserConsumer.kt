package no.nav.utenlandsadresser.adapter.inbound.kafka

interface LivshendelserConsumer {
    suspend fun consumePersonhendelser()
}
