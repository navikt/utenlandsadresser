plugins {
    kotlin("jvm")
    idea
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":app"))

    implementation(libs.bundles.jetbrainsExposed)

    implementation(libs.jetbrainsKotlinxDatetime)

    testImplementation(testLibs.bundles.kotest)
    testImplementation(testLibs.testContainersPostgres)

    testImplementation(libs.postgresql)

    testImplementation(libs.flywayCore)
    testRuntimeOnly(libs.flywayDatabasePostgres)
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        val appProject = project(":app")
        val migrationsDir = file("${appProject.projectDir}/src/main/resources/db/migration")
        if (!migrationsDir.exists()) {
            throw GradleException("Migrations directory does not exist: $migrationsDir")
        }
        classpath += files(migrationsDir)
    }
}
kotlin {
    jvmToolchain(17)
}

// tasks.register<Copy>("copyFlywayMigrations") {
//    from("${project(":app").projectDir}/src/main/resources/db/migration")
//    into("${layout.projectDirectory}/src/main/resources/db/migration")
// }
