package no.nav.utenlandsadresser.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer

fun kafkConsumerConfig(config: KafkaConfig) =
    mapOf(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to config.brokers,
        ConsumerConfig.GROUP_ID_CONFIG to config.groupId,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
        "schema.registry.url" to config.schemaRegistry,
        "basic.auth.credentials.source" to "USER_INFO",
        "schema.registry.basic.auth.user.info" to "${config.schemaRegistryUser}:${config.schemaRegistryPassword.value}",
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to config.truststorePath,
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to config.credstorePassword.value,
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to config.keystorePath,
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to config.credstorePassword.value,
        SslConfigs.SSL_KEY_PASSWORD_CONFIG to config.credstorePassword.value,
    )
