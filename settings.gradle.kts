// Apply plugins
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "utenlandsadresser"
include("app")
include("sporingslogg-cleanup")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
    }
    versionCatalogs {
        create("libs") {
            version("kotlinGradlePlugin", "2.0.0")

            version("jetbrainsExposed", "0.52.0")

            library("jetbrainsExposedCore", "org.jetbrains.exposed", "exposed-core").versionRef("jetbrainsExposed")
            library("jetbrainsExposedJdbc", "org.jetbrains.exposed", "exposed-jdbc").versionRef("jetbrainsExposed")
            library(
                "jetbrainsExposedKotlinDatetime",
                "org.jetbrains.exposed",
                "exposed-kotlin-datetime",
            ).versionRef("jetbrainsExposed")
            library("jetbrainsExposedJson", "org.jetbrains.exposed", "exposed-json").versionRef("jetbrainsExposed")

            bundle(
                "jetbrainsExposed",
                listOf(
                    "jetbrainsExposedCore",
                    "jetbrainsExposedJdbc",
                    "jetbrainsExposedKotlinDatetime",
                    "jetbrainsExposedJson",
                ),
            )

            library(
                "jetbrainsKotlinxCoroutinesCore",
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core",
            ).version("1.8.1")

            library(
                "jetbrainsKotlinxDatetime",
                "org.jetbrains.kotlinx",
                "kotlinx-datetime",
            ).version("0.6.0")

            library("postgresql", "org.postgresql", "postgresql").version("42.7.3")

            version("flyway", "10.15.2")
            library("flywayCore", "org.flywaydb", "flyway-core").versionRef("flyway")
            library("flywayDatabasePostgres", "org.flywaydb", "flyway-database-postgresql").versionRef("flyway")
        }

        create("testLibs") {

            version("kotest", "5.9.1")
            library("kotestRunnerJunit5", "io.kotest", "kotest-runner-junit5-jvm").versionRef("kotest")
            library("kotestAssertionsCore", "io.kotest", "kotest-assertions-core-jvm").versionRef("kotest")
            library("kotestAssertionsJson", "io.kotest", "kotest-assertions-json-jvm").versionRef("kotest")
            library("kotestFrameworkEngine", "io.kotest", "kotest-framework-engine").versionRef("kotest")
            library("kotestFrameworkDatatest", "io.kotest", "kotest-framework-datatest").versionRef("kotest")
            library("kotestExtensions", "io.kotest", "kotest-extensions-jvm").versionRef("kotest")

            library("kotestExtensionsWiremock", "io.kotest.extensions", "kotest-extensions-wiremock").version("3.1.0")
            library(
                "kotestTestcontainersExtension",
                "io.kotest.extensions",
                "kotest-extensions-testcontainers",
            ).version("2.0.2")

            bundle(
                "kotest",
                listOf(
                    "kotestRunnerJunit5",
                    "kotestAssertionsCore",
                    "kotestAssertionsJson",
                    "kotestFrameworkEngine",
                    "kotestFrameworkDatatest",
                    "kotestExtensions",
                    "kotestExtensionsWiremock",
                    "kotestTestcontainersExtension",
                ),
            )

            version("testcontainers", "1.19.8")
            library("testContainersPostgres", "org.testcontainers", "postgresql").versionRef("testcontainers")
            library("testContainersKafka", "org.testcontainers", "kafka").versionRef("testcontainers")
        }
    }
}
