import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("io.ktor.plugin") version "2.3.8"
}

group = "no.nav.utenlandsadresser"

version = "0.0.1"
application {
    mainClass.set("no.nav.utenlandsadresser.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation("io.ktor:ktor-client-logging-jvm:2.3.7")
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-openapi:$ktorVersion")
    constraints {
        // Transitive dependencies of ktor-server-openapi
        implementation("org.json:json:20231013") {
            because("Previous versions have security vulnerabilities")
        }
        implementation("com.google.guava:guava:32.1.3-android") {
            because("Previous versions have security vulnerabilities")
        }
    }

    runtimeOnly("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    val exposedVersion = "0.46.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    implementation("org.apache.kafka:kafka-clients:3.6.1")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
    constraints {
        implementation("org.apache.avro:avro:1.11.3") {
            because("Previous versions have security vulnerabilities")
        }
        implementation("org.apache.commons:commons-compress:1.26.0") {
            because("Previous versions have security vulnerabilities")
        }
    }
    implementation("com.github.avro-kotlin.avro4k:avro4k-core:1.10.0")

    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")

    implementation("io.github.smiley4:ktor-swagger-ui:2.7.4")

    val flywayVersion = "10.7.1"
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    val arrowVersion = "1.2.1"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrowVersion")

    implementation("com.h2database:h2:2.2.220")

    val hopliteVersion = "2.7.5"
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")

    val kotestVersion = "5.8.0"
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-engine:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-jvm:$kotestVersion")

    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
    testImplementation("org.testcontainers:kafka:1.19.6")
    implementation("org.testcontainers:postgresql:1.19.7")

    testImplementation("org.wiremock:wiremock:3.3.1")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:2.0.1")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")

    testImplementation("io.mockk:mockk:1.13.8")
}


tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xcontext-receivers")
        }
    }

    // Trengs for å løse en feil med Flyway der dependencies ikke blir slått sammen riktig
    withType<ShadowJar> {
        mergeServiceFiles()
    }

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
