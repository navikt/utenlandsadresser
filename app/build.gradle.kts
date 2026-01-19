plugins {
    kotlin("jvm")
    idea
    kotlin("plugin.serialization")
    alias(libs.plugins.ktor)
    id("com.autonomousapps.dependency-analysis")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
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
    implementation(libs.hopliteCore)
    implementation(libs.hopliteHocon)

    // Logging (shared)
    implementation(libs.slf4jApi)
    implementation(libs.logback)
    runtimeOnly(libs.log4jCore)

    // Testing (shared)
    runtimeOnly(libs.kotestRunnerJunit5)
    implementation(libs.kotestAssertionsShared)
    implementation(libs.kotestFrameworkApi)
    testImplementation(libs.kotestAssertionsCore)
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
    val kotlinxVerion = "1.10.2"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVerion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVerion")

    // Database
    implementation("org.postgresql:postgresql:42.7.9")
    implementation("com.zaxxer:HikariCP:7.0.2")

    // Exposed
    val exposedVersion = "0.61.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    // Flyway
    val flywayVersion = "11.20.2"
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:4.0.0")
    implementation("io.confluent:kafka-avro-serializer:8.1.1")
    implementation("org.apache.avro:avro:1.12.1") {
        because("Security vulnerabilities in avro < 1.11.4")
    }
    implementation("com.github.avro-kotlin.avro4k:avro4k-core:2.8.0")

    // Logging Additional
    implementation("ch.qos.logback:logback-core:${libs.versions.logback.get()}")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    // Metrics
    val micromenterVersion = "1.16.2"
    implementation("io.micrometer:micrometer-core:$micromenterVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micromenterVersion")

    // OpenAPI
    val smileyVersion = "5.4.0"
    implementation("io.github.smiley4:ktor-openapi:$smileyVersion")
    implementation("io.github.smiley4:ktor-swagger-ui:$smileyVersion")
    constraints {
        // Transitive dependencies of ktor-server-openapi
        implementation("org.json:json:20250517") {
            because("Previous versions have security vulnerabilities")
        }
    }

    // Arrow
    implementation("io.arrow-kt:arrow-core:2.2.1.1")

    // JWT
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.auth0:jwks-rsa:0.23.0")

    // Testing Additional
    val kotestVersion = libs.versions.kotest.get()
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-jvm:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
    testImplementation("io.kotest:kotest-assertions-api:$kotestVersion")
    testImplementation("io.kotest:kotest-common:$kotestVersion")

    // Testcontainers
    val testcontainersVersion = "1.21.4"
    implementation("org.testcontainers:postgresql:$testcontainersVersion")
    implementation("org.testcontainers:testcontainers:$testcontainersVersion")

    // Wiremock
    testImplementation("org.wiremock:wiremock:3.13.2")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:3.1.0") {
        exclude(group = "org.wiremock", module = "wiremock-standalone")
    }
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
}
