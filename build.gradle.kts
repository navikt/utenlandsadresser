plugins {
    kotlin("jvm") version "2.0.21" apply false
    id("com.autonomousapps.dependency-analysis") version "2.4.2" apply false
}

subprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper> {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }
}
