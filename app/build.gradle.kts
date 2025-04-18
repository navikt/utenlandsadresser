import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm")
    idea
    kotlin("plugin.serialization") version "2.1.20"
    alias(libs.plugins.ktor)
    id("com.autonomousapps.dependency-analysis")
}

application {
    mainClass.set("no.nav.utenlandsadresser.ApplicationKt")

    val isDevelopment: Boolean = project.hasProperty("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.bundles.ktorClient)
    val ktorVersion = libs.versions.ktor.get()

    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-openapi:$ktorVersion")
    constraints {
        // Transitive dependencies of ktor-server-openapi
        implementation("org.json:json:20250107") {
            because("Previous versions have security vulnerabilities")
        }
    }

    runtimeOnly(libs.jetbrainsKotlinxDatetime)

    implementation(libs.bundles.jetbrainsExposed)

    implementation("org.apache.kafka:kafka-clients:3.8.0")
    implementation("io.confluent:kafka-avro-serializer:7.9.0")
    constraints {
        implementation("org.apache.avro:avro:1.12.0") {
            because("Security vulnerabilities in avro < 1.11.4")
        }
    }

    implementation("com.github.avro-kotlin.avro4k:avro4k-core:2.3.0")

    implementation(libs.postgresql)
    implementation("com.zaxxer:HikariCP:6.3.0")

    implementation(libs.bundles.logging)

    implementation("io.micrometer:micrometer-registry-prometheus:1.14.6")

    implementation("io.github.smiley4:ktor-openapi:5.0.2")
    implementation("io.github.smiley4:ktor-swagger-ui:5.0.2")

    implementation(libs.flywayCore)
    runtimeOnly(libs.flywayDatabasePostgres)

    val arrowVersion = "2.0.1"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrowVersion")

    implementation("com.h2database:h2:2.3.232")

    implementation(libs.bundles.hoplite)

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    testImplementation(libs.bundles.kotest)

    implementation(libs.testContainersPostgres)
    testImplementation(libs.testContainersKafka)

    testImplementation("org.wiremock:wiremock:3.12.1")
    testImplementation(libs.kotestExtensionsWiremock)
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")

    testImplementation(libs.mockk)
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
        // Required for testing environment variables
        jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
    }
}
