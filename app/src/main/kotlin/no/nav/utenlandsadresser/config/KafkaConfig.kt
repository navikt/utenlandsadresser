package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked

data class KafkaConfig(
    val brokers: String,
    val schemaRegistry: String,
    val schemaRegistryUser: String,
    val schemaRegistryPassword: Masked,
    val credstorePassword: Masked,
    val keystorePath: String,
    val truststorePath: String,
    val groupId: String,
    val topic: String,
)
