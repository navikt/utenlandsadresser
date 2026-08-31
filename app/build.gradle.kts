plugins {
    kotlin("jvm")
    idea
    kotlin("plugin.serialization")
    alias(libs.plugins.ktor)
    id("com.autonomousapps.dependency-analysis")
    alias(libs.plugins.kotest)
}

application {
    mainClass.set("no.nav.utenlandsadresser.ApplicationKt")

    val isDevelopment: Boolean = project.hasProperty("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    constraints {
        implementation("org.json:json:[20260814,]")
        implementation("io.netty:netty-codec-dns:[4.2.15,5)")
        implementation("io.netty:netty-codec-http:[4.2.17.Final,5)")
        implementation("io.netty:netty-resolver-dns:[4.2.15,5)")
        implementation("io.netty:netty-handler-proxy:[4.2.13,5)")
        implementation("com.ongres.scram:scram-common:[3.3,4)")
        implementation("com.ongres.scram:scram-client:[3.3,4)")
        implementation("org.apache.httpcomponents.core5:httpcore5:(5.4.2,6)")
        implementation("org.apache.httpcomponents.core5:httpcore5-h2:(5.4.2,6)")
        implementation("org.apache.httpcomponents.client5:httpclient5:[5.6.3,6)")
        implementation("at.yawk.lz4:lz4-java:[1.11.1,2)")
        implementation("org.mozilla:rhino:[1.8.1,2)")
    }
    // Shared dependencies from the version catalog

    // Ktor Client (shared)
    implementation(libs.bundles.ktorClient)

    // Ktor Client (module-specific)
    val ktorVersion = libs.versions.ktor.get()
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Ktor Common (shared)
    implementation(libs.ktorHttp)
    implementation(libs.ktorUtils)
    implementation(libs.ktorSerialization)

    // Kotlinx (shared)
    implementation(libs.bundles.kotlinxSerialization)
    testImplementation(libs.kotlinxSerializationJson)

    // Configuration (shared)
    implementation(libs.hopliteCore)
    runtimeOnly(libs.hopliteHocon)

    // Logging (shared)
    implementation(libs.slf4jApi)
    implementation(libs.logback)
    runtimeOnly(libs.log4jCore)

    // Testing (shared)
    testRuntimeOnly(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestFrameworkEngine)
    testImplementation(libs.kotestAssertionsShared)
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
    val kotlinxVerion = "1.11.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVerion")

    // Database
    runtimeOnly("org.postgresql:postgresql:42.7.13")
    implementation("org.postgresql:r2dbc-postgresql:1.1.2.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:1.0.2.RELEASE")
    implementation("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")

    // Exposed
    val exposedVersion = "1.5.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-r2dbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    // Flyway
    val flywayVersion = "13.4.0"
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:4.3.1")
    implementation("io.confluent:kafka-avro-serializer:8.3.1")
    implementation("org.apache.avro:avro:1.12.2")

    implementation("com.github.avro-kotlin.avro4k:avro4k-core:2.12.0")

    // Logging Additional
    implementation("ch.qos.logback:logback-core:${libs.versions.logback.get()}")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    // Metrics
    val micromenterVersion = "1.17.1"
    implementation("io.micrometer:micrometer-core:$micromenterVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micromenterVersion")

    // OpenAPI
    val smileyVersion = "5.7.0"
    implementation("io.github.smiley4:ktor-openapi:$smileyVersion")
    implementation("io.github.smiley4:ktor-swagger-ui:$smileyVersion")

    // Arrow
    val arrowVersion = "2.2.3"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-exception-utils:$arrowVersion")

    // JWT
    implementation("com.auth0:java-jwt:4.6.0")
    implementation("com.auth0:jwks-rsa:0.24.1")

    // Testing Additional
    val kotestVersion = libs.versions.kotest.get()
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-testcontainers:$kotestVersion")
    testImplementation("io.kotest:kotest-common:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-wiremock:$kotestVersion") {
        exclude(group = "org.wiremock", module = "wiremock-standalone")
    }
    // Testcontainers
    val testcontainersVersion = "2.0.5"
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")

    // Wiremock
    val wiremock = "3.13.2"
    testImplementation("org.wiremock:wiremock:$wiremock")
    testImplementation("org.wiremock:wiremock-standalone:$wiremock")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1") {
        exclude(group = "org.wiremock", module = "wiremock-standalone")
    }
}
