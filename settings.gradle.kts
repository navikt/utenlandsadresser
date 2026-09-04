import java.net.URI

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
        maven {
            name = "pdl-github"
            url = URI("https://maven.pkg.github.com/navikt/pdl")
            credentials {
                username = providers.gradleProperty("maven.github.pdl.username").orElse("x-access-token").orNull
                password =
                    providers
                        .gradleProperty("maven.github.pdl.password")
                        .orElse(providers.environmentVariable("READER_TOKEN"))
                        .orNull
            }
        }
    }
}
