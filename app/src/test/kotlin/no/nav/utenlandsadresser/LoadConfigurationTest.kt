package no.nav.utenlandsadresser

import io.kotest.core.spec.style.WordSpec
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.equals.shouldBeEqual
import no.nav.utenlandsadresser.setup.loadConfiguration

class LoadConfigurationTest :
    WordSpec({
        "load configuration" should {
            "load configuration for prod" {
                withEnvironment(applicationEnvironment) {
                    val config = loadConfiguration(AppEnv.PROD_GCP)

                    config.registeroppslag.baseUrl shouldBeEqual "https://regoppslag.prod-fss-pub.nais.io"
                    config.registeroppslag.cluster shouldBeEqual "prod-fss"
                    config.registeroppslag.scope shouldBeEqual "api://prod-fss.teamdokumenthandtering.regoppslag/.default"
                    config.maskinporten.consumers shouldBeEqual listOf("974761076")
                }
            }

            "load configuration for dev" {
                withEnvironment(applicationEnvironment) {
                    val config = loadConfiguration(AppEnv.DEV_GCP)

                    config.registeroppslag.baseUrl shouldBeEqual "https://regoppslag.dev-fss-pub.nais.io"
                    config.registeroppslag.cluster shouldBeEqual "dev-fss"
                    config.registeroppslag.scope shouldBeEqual "api://dev-fss.teamdokumenthandtering.regoppslag/.default"
                    config.maskinporten.consumers shouldBeEqual listOf("889640782", "974761076")
                }
            }
        }
    })

private val applicationEnvironment: Map<String, String> =
    mapOf(
        "MASKINPORTEN_CLIENT_ID" to "clientId",
        "MASKINPORTEN_CLIENT_JWK" to "clientJwk",
        "MASKINPORTEN_SCOPES" to "scopes",
        "MASKINPORTEN_WELL_KNOWN_URL" to "wellKnownUrl",
        "MASKINPORTEN_ISSUER" to "issuer",
        "MASKINPORTEN_TOKEN_ENDPOINT" to "tokenEndpoint",
        "MASKINPORTEN_JWKS_URI" to "jwksUri",
        "NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_HOST" to "host",
        "NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_PORT" to "port",
        "NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_DATABASE" to "name",
        "NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_USERNAME" to "username",
        "NAIS_DATABASE_UTENLANDSADRESSER_UTENLANDSADRESSER_PASSWORD" to "password",
        "AZURE_APP_CLIENT_ID" to "clientId",
        "AZURE_APP_CLIENT_SECRET" to "clientSecret",
        "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "tokenEndpoint",
        "BEHANDLINGSKATALOG_BEHANDLINGSNUMMER" to "behandlingsnummer",
        "KAFKA_BROKERS" to "brokers",
        "KAFKA_SCHEMA_REGISTRY" to "schemaRegistry",
        "KAFKA_SCHEMA_REGISTRY_USER" to "schemaRegistryUser",
        "KAFKA_SCHEMA_REGISTRY_PASSWORD" to "schemaRegistryPassword",
        "KAFKA_CERTIFICATE" to "certificate",
        "KAFKA_CERTIFICATE_PATH" to "certificatePath",
        "KAFKA_PRIVATE_KEY" to "privateKey",
        "KAFKA_PRIVATE_KEY_PATH" to "privateKeyPath",
        "KAFKA_CA" to "ca",
        "KAFKA_CA_PATH" to "caPath",
        "KAFKA_CREDSTORE_PASSWORD" to "credstorePassword",
        "KAFKA_KEYSTORE_PATH" to "keystorePath",
        "KAFKA_TRUSTSTORE_PATH" to "truststorePath",
        "AIVEN_SECRET_UPDATED" to "aivenSecretUpdated",
    )
