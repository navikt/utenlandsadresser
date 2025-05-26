// Apply plugins
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
