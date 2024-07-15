plugins {
    kotlin("jvm")
    idea
}

dependencies {
    implementation(project(":app"))
    implementation(libs.bundles.ktorClient)
    implementation(libs.bundles.hoplite)

    testImplementation(testLibs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
