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
    implementation(libs.bundles.hoplite)

    // Logging (shared)
    implementation(libs.bundles.logging)

    // Testing (shared)
    implementation(libs.bundles.kotest)
    testImplementation(libs.bundles.mocking)
}
