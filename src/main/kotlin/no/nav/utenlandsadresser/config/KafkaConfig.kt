package no.nav.utenlandsadresser.config

import com.sksamuel.hoplite.Masked

data class KafkaConfig(
    val brokers: String,
    val schemaRegistry: String,
    val schemaRegistryUser: String,
    val schemaRegistryPassword: Masked,
    val certificate: Masked,
    val certificatePath: Masked,
    val privateKey: Masked,
    val privateKeyPath: Masked,
    val ca: Masked,
    val caPath: Masked,
    val credstorePassword: Masked,
    val keystorePath: Masked,
    val truststorePath: String,
    val aivenSecretUpdated: String,
    val groupId: String,
    val topic: String,
)
