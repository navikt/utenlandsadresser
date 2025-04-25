plugins {
    kotlin("jvm")
    idea
    kotlin("plugin.serialization")
    alias(libs.plugins.ktor)
    id("com.autonomousapps.dependency-analysis")
}

application {
    mainClass.set("no.nav.utenlandsadresser.ApplicationKt")

    val isDevelopment: Boolean = project.hasProperty("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    // Shared dependencies from the version catalog

    // Ktor Client (shared)
    implementation(libs.bundles.ktorClient)

    // Ktor Client (module-specific)
    val ktorVersion = libs.versions.ktor.get()
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Ktor Common (shared)
    implementation(libs.ktorHttp)
    testImplementation(libs.ktorHttp)
    implementation(libs.ktorUtils)
    testImplementation(libs.ktorUtils)
    implementation(libs.ktorSerialization)

    // Kotlinx (shared)
    implementation(libs.jetbrainsKotlinxDatetime)
    implementation(libs.bundles.kotlinxSerialization)
    testImplementation(libs.kotlinxSerializationJson)

    // Configuration (shared)
    implementation(libs.bundles.hoplite)

    // Logging (shared)
    implementation(libs.bundles.logging)

    // Testing (shared)
    implementation(libs.bundles.kotest)
    testImplementation(libs.bundles.mocking)

    // Module-specific dependencies

    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    // Ktor Additional
    implementation("io.ktor:ktor-io:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")

    // Kotlinx Additional
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Database
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.zaxxer:HikariCP:6.3.0")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.61.0")
    implementation("org.jetbrains.exposed:exposed-json:0.61.0")

    // Flyway
    implementation("org.flywaydb:flyway-core:11.8.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.8.0")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:4.0.0")
    implementation("io.confluent:kafka-avro-serializer:7.9.0")
    implementation("org.apache.avro:avro:1.12.0") {
        because("Security vulnerabilities in avro < 1.11.4")
    }
    implementation("com.github.avro-kotlin.avro4k:avro4k-core:2.3.0")

    // Logging Additional
    implementation("ch.qos.logback:logback-core:${libs.versions.logback.get()}")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    // Metrics
    implementation("io.micrometer:micrometer-core:1.14.6")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.6")

    // OpenAPI
    implementation("io.github.smiley4:ktor-openapi:5.0.2")
    implementation("io.github.smiley4:ktor-swagger-ui:5.0.2")
    constraints {
        // Transitive dependencies of ktor-server-openapi
        implementation("org.json:json:20250107") {
            because("Previous versions have security vulnerabilities")
        }
    }

    // Arrow
    implementation("io.arrow-kt:arrow-core:2.1.0")

    // JWT
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.auth0:jwks-rsa:0.22.1")

    // Testing Additional
    val kotestVersion = libs.versions.kotest.get()
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-jvm:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
    testImplementation("io.kotest:kotest-assertions-api:$kotestVersion")
    testImplementation("io.kotest:kotest-common:$kotestVersion")

    // Testcontainers
    implementation("org.testcontainers:postgresql:1.21.0")
    implementation("org.testcontainers:testcontainers:1.21.0")

    // Wiremock
    testImplementation("org.wiremock:wiremock:3.13.0")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:3.1.0")
    testImplementation("org.wiremock:wiremock-standalone:3.13.0")
}
