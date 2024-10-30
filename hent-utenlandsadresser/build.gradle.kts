plugins {
    kotlin("jvm")
    idea
    application
    id("com.autonomousapps.dependency-analysis")
}

application {
    mainClass = "no.nav.utenlandsadresser.hent.utenlandsadresser.MainKt"
}

dependencies {
    implementation(project(":app"))
    implementation(libs.bundles.ktorClient)
}

tasks.test {
    useJUnitPlatform()
}