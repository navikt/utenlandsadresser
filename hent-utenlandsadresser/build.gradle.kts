plugins {
    kotlin("jvm")
    idea
    application
}

application {
    mainClass = "no.nav.utenlandsadresser.hent.utenlandsadresser.MainKt"
}

tasks.test {
    useJUnitPlatform()
}