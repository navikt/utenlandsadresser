// Apply plugins
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "utenlandsadresser"
include("app")
include("sporingslogg-cleanup")
include("hent-utenlandsadresser")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
    }
}
