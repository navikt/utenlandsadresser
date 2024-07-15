plugins {
    kotlin("jvm")
    idea
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass = "no.nav.utenlandsadresser.sporingslogg.cleanup.MainKt"
}

dependencies {
    implementation(project(":app"))
    implementation(libs.bundles.ktorClient)
    implementation(libs.bundles.hoplite)
}

kotlin {
    jvmToolchain(17)
}
