plugins {
    kotlin("jvm")
    idea
    application
    id("com.autonomousapps.dependency-analysis")
}

application {
    mainClass = "no.nav.utenlandsadresser.sporingslogg.cleanup.MainKt"
}

dependencies {
    implementation(project(":app"))

    // Shared dependencies from the version catalog

    // Ktor Client (shared)
    implementation(libs.bundles.ktorClient)
    testImplementation(libs.ktorClientMock)

    // Ktor Common (shared)
    implementation(libs.ktorHttp)
    testImplementation(libs.ktorHttp)

    // Configuration (shared)
    implementation(libs.hopliteCore)
    runtimeOnly(libs.hopliteHocon)

    // Logging (shared)
    implementation(libs.slf4jApi)
    runtimeOnly(libs.logback)
    runtimeOnly(libs.log4jCore)

    // Testing (shared)
    runtimeOnly(libs.kotestRunnerJunit5)
    implementation(libs.kotestAssertionsShared)
    implementation(libs.kotestFrameworkApi)
    testImplementation(libs.kotestAssertionsCore)
    testImplementation(libs.bundles.mocking)
}
