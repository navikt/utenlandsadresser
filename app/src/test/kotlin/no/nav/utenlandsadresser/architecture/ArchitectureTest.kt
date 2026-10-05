package no.nav.utenlandsadresser.architecture

import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import io.kotest.core.spec.style.WordSpec

class ArchitectureTest :
    WordSpec({
        "domain layer" should {
            "only depend on the Kotlin and Java standard libraries" {
                domainOnlyDependsOnStandardLibraries.check(importedClasses)
            }
        }
        "application layer" should {
            "only depend on domain, the standard libraries, Arrow and SLF4J" {
                applicationOnlyDependsOnDomainAndLibraries.check(importedClasses)
            }
        }
        "adapters" should {
            "not depend on each other" {
                adaptersDoNotDependOnEachOther.check(importedClasses)
            }
        }
        "inbound adapters" should {
            "only use inbound ports from the application layer" {
                inboundAdaptersOnlyUseInboundPorts.check(importedClasses)
            }
        }
    }) {
    companion object {
        private val importedClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages("no.nav.utenlandsadresser")

        private val standardLibraries =
            arrayOf(
                "java..",
                "kotlin..",
                "org.jetbrains.annotations..",
            )

        /** `DevRoute` finnes bare i dev og lokalt, og kaller oppslagene og Maskinporten direkte for feilsøking. */
        private val erDevRoute =
            nameMatching("""no\.nav\.utenlandsadresser\.adapter\.inbound\.web\.DevRoute.*""")

        private val domainOnlyDependsOnStandardLibraries =
            classes()
                .that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", *standardLibraries)

        private val applicationOnlyDependsOnDomainAndLibraries =
            classes()
                .that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                    "..domain..",
                    "..application..",
                    "arrow..",
                    "org.slf4j..",
                    *standardLibraries,
                )

        /**
         * Hver pakke to nivåer under `adapter` er en egen adapter, for eksempel `inbound.web` og
         * `outbound.persistence`. `adapter.health` ligger bare ett nivå ned og er felles for web og kafka.
         */
        private val adaptersDoNotDependOnEachOther =
            slices()
                .matching("no.nav.utenlandsadresser.adapter.(*).(*)..")
                .should().notDependOnEachOther()
                .ignoreDependency(erDevRoute, nameMatching("""no\.nav\.utenlandsadresser\.adapter\.outbound\.maskinporten\..*"""))

        private val inboundAdaptersOnlyUseInboundPorts =
            noClasses()
                .that().resideInAPackage("..adapter.inbound..")
                .and(not(erDevRoute))
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "..application.port.outbound..",
                    "..application.service..",
                    "..adapter.outbound..",
                )
    }
}
