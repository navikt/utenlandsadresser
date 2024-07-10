package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked

data class KafkaConfig(
    val brokers: String,
    val schemaRegistry: String,
    val schemaRegistryUser: String,
    val schemaRegistryPassword: Masked,
    val certificate: Masked,
    val certificatePath: String,
    val privateKey: Masked,
    val privateKeyPath: String,
    val ca: Masked,
    val caPath: String,
    val credstorePassword: Masked,
    val keystorePath: String,
    val truststorePath: String,
    val aivenSecretUpdated: String,
    val groupId: String,
    val topic: String,
)
