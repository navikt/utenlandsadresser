plugins {
    kotlin("jvm")
    idea
    application
    kotlin("plugin.serialization")
    id("com.autonomousapps.dependency-analysis")
}

application {
    mainClass = "no.nav.utenlandsadresser.hent.utenlandsadresser.MainKt"
}

dependencies {
    implementation(project(":app"))

    // Shared dependencies from the version catalog

    // Ktor Client (shared)
    implementation(libs.bundles.ktorClient)

    // Ktor Common (shared)
    implementation(libs.ktorSerialization)
    implementation(libs.ktorHttp)
    implementation(libs.ktorUtils)

    // Configuration (shared)
    implementation(libs.hopliteCore)
    runtimeOnly(libs.hopliteHocon)

    // Kotlinx (shared)
    implementation(libs.jetbrainsKotlinxDatetime)
    implementation(libs.bundles.kotlinxSerialization)
    testImplementation(libs.bundles.kotlinxSerialization)

    // Logging (shared)
    implementation(libs.slf4jApi)

    // Testing (shared)
    testRuntimeOnly(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestFrameworkApi)
}
