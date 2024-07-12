plugins {
    kotlin("jvm")
    idea
}

dependencies {
    testImplementation(testLibs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
