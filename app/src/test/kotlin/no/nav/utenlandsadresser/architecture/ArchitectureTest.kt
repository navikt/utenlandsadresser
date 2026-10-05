package no.nav.utenlandsadresser.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.WordSpec

class ArchitectureTest :
    WordSpec({
        "domain layer" should {
            "not depend on application or adapter layers" {
                domainDoesNotDependOnOuterLayers.check(importedClasses)
            }
        }
        "application layer" should {
            "not depend on adapters or framework setup" {
                applicationDoesNotDependOnAdapters.check(importedClasses)
            }
        }
    }) {
    companion object {
        private val importedClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages("no.nav.utenlandsadresser")

        private val domainDoesNotDependOnOuterLayers =
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "..application..",
                    "..adapter..",
                    "..setup..",
                    "..config..",
                )

        private val applicationDoesNotDependOnAdapters =
            noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "..adapter..",
                    "..setup..",
                    "..config..",
                )
    }
}
