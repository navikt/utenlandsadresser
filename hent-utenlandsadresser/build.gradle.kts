plugins {
    kotlin("jvm")
    idea
    application
    kotlin("plugin.serialization") version "2.1.20"
    id("com.autonomousapps.dependency-analysis")
}

application {
    mainClass = "no.nav.utenlandsadresser.hent.utenlandsadresser.MainKt"
}

dependencies {
    val ktorVersion = libs.versions.ktor.get()
    implementation(project(":app"))
    implementation(libs.bundles.ktorClient)
    implementation(libs.bundles.hoplite)
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation(libs.jetbrainsKotlinxDatetime)

    testImplementation(libs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}
