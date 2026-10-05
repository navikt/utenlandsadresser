plugins {
    kotlin("jvm")
    idea
    kotlin("plugin.serialization")
    id("com.autonomousapps.dependency-analysis")
}

dependencies {
    val ktorVersion = libs.versions.ktor.get()

    // Typer i offentlige signaturer: HttpClient, Masked, JsonElement
    api(libs.ktorClientCore)
    api(libs.hopliteCore)
    api(libs.kotlinxSerializationJson)

    // Ktor Client
    implementation(libs.ktorClientCio)
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation(libs.ktorHttp)
    implementation(libs.ktorSerialization)

    // Kotlinx
    implementation(libs.kotlinxSerializationCore)

    // Arrow
    implementation(libs.arrowCore)

    // Logging
    implementation(libs.slf4jApi)
    implementation(libs.logback)
    implementation(libs.logbackCore)
    implementation(libs.logstashLogbackEncoder)
}
