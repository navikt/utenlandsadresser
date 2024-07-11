package no.nav.utenlandsadresser.sporingslogg.cleanup.infrastructure.postgres

import io.kotest.core.extensions.install
import io.kotest.core.spec.DoNotParallelize
import io.kotest.core.spec.style.WordSpec
import io.kotest.extensions.testcontainers.ContainerExtension
import io.kotest.extensions.testcontainers.ContainerLifecycleMode
import io.kotest.matchers.shouldBe
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.SporingsloggPostgresRepository
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.days

private val sqlContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))

private lateinit var flyway: Flyway

@DoNotParallelize
class DeleteSporingsloggerOlderThanTest :
    WordSpec({
        install(
            ContainerExtension(
                sqlContainer,
                mode = ContainerLifecycleMode.Project,
                afterStart = {
                    flyway =
                        Flyway
                            .configure()
                            .dataSource(sqlContainer.jdbcUrl, sqlContainer.username, sqlContainer.password)
                            .cleanDisabled(false)
                            .load()!!
                },
                beforeTest = {
                    flyway.migrate()
                },
                afterTest = {
                    flyway.clean()
                },
            ),
        )

        val database =
            Database.connect(
                url = sqlContainer.jdbcUrl,
                user = sqlContainer.username,
                password = sqlContainer.password,
                driver = org.postgresql.Driver::class.qualifiedName!!,
            )

        val sporingsloggRepository = SporingsloggPostgresRepository(database)

        "delete sporingslogg older than 30 days" should {
            "delete sporingslogg older than 30 days" {
                sporingsloggRepository.deleteSporingsloggerOlderThan(30.days)

                val sporingslogger = sporingsloggRepository.getSporingslogger(Identitetsnummer("12345678910"))

                sporingslogger.size shouldBe 0
            }
        }
    })
