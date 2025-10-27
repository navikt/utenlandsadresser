plugins {
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    id("com.autonomousapps.dependency-analysis") version "3.3.0" apply false
    id("com.github.ben-manes.versions") version "0.53.0" apply false
}

subprojects {
    // Apply common configurations to all subprojects
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper> {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }

    // Apply the versions plugin to all subprojects
    apply(plugin = "com.github.ben-manes.versions")

    // Configure the version plugin
    tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
        // Reject unstable versions
        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }

        // Output format
        outputFormatter = "json,xml,html"

        // Check for updates every time the project is built
        checkForGradleUpdate = true
        checkConstraints = true
        checkBuildEnvironmentConstraints = true
    }

    // Common test configuration for all subprojects
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        // Required for testing environment variables
        jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
    }
}

// Helper function to check if a version is unstable
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return !isStable
}
