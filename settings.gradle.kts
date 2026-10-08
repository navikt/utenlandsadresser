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
                username = "x-access-token"
                password =
                    providers.gradleProperty("gpr.token").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }
    }
}
