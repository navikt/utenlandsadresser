import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    id("io.ktor.plugin") version "2.3.7"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21"
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

    val exposedVersion = "0.41.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation("com.h2database:h2:2.2.224")

    implementation("ch.qos.logback:logback-classic:1.4.14")

    implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")

    val arrowVersion = "1.2.1"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrowVersion")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")

    val kotestVersion = "5.8.0"
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-engine:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-jvm:$kotestVersion")

    testImplementation("org.wiremock:wiremock:3.3.1")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:2.0.1")
    testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")

    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xcontext-receivers")
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
}

