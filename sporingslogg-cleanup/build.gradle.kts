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
    implementation(libs.bundles.logging)

    testImplementation(testLibs.bundles.kotest)
    testImplementation(testLibs.mockk)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    test {
        useJUnitPlatform()
        // Required for testing environment variables
        jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
    }
}
