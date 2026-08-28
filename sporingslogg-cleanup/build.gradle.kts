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
    constraints {
        implementation("org.json:json:[20260814,]")
        implementation("io.netty:netty-codec-dns:[4.2.15,5)")
        implementation("io.netty:netty-codec-http:[4.2.17.Final,5)")
        implementation("io.netty:netty-resolver-dns:[4.2.15,5)")
        implementation("io.netty:netty-handler-proxy:[4.2.13,5)")
        implementation("com.ongres.scram:scram-common:[3.3,4)")
        implementation("com.ongres.scram:scram-client:[3.3,4)")
        implementation("org.apache.httpcomponents.core5:httpcore5:(5.4.2,6)")
        implementation("org.apache.httpcomponents.core5:httpcore5-h2:(5.4.2,6)")
        implementation("org.apache.httpcomponents.client5:httpclient5:[5.6.3,6)")
        implementation("at.yawk.lz4:lz4-java:[1.11.1,2)")
        implementation("org.mozilla:rhino:[1.8.1,2)")
    }
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
    testImplementation(libs.kotestAssertionsShared)
    testImplementation(libs.kotestFrameworkEngine)
    testImplementation(libs.kotestAssertionsCore)
    testImplementation(libs.bundles.mocking)
}
