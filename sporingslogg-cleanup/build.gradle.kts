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
    implementation(libs.bundles.ktorClient)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.logging)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}

tasks {
    test {
        useJUnitPlatform()
        // Required for testing environment variables
        jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
    }
}
