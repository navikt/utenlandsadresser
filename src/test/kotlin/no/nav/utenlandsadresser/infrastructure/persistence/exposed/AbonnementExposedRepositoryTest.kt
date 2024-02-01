package no.nav.utenlandsadresser.infrastructure.persistence.exposed

import arrow.core.getOrElse
import io.kotest.assertions.fail
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.dto.AbonnementDto
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Fødselsnummer
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class AbonnementExposedRepositoryTest : WordSpec({
    val dataSourceUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;"
    val database = Database.connect(dataSourceUrl, driver = "org.h2.Driver")

    val abonnementRepository = AbonnementExposedRepository(database)
    lateinit var flyway: Flyway

    beforeTest {
        flyway = Flyway.configure()
            .dataSource(dataSourceUrl, "", "")
            .cleanDisabled(false)
            .load()

        flyway.migrate()
    }

    afterTest {
        flyway.clean()
    }

    "create abonnement" should {
        "insert a new abonnement" {
            val abonnement = Abonnement(
                clientId = ClientId("test-client-id"),
                fødselsnummer = Fødselsnummer("12345678910").getOrElse { fail("Invalid fødselsnummer") },
                løpenummer = 0,
                opprettet = Clock.System.now(),
            )

            abonnementRepository.createAbonnement(abonnement)

            with(database) {
                abonnementRepository.getAbonnement(abonnement.fødselsnummer, abonnement.clientId) shouldBe abonnement
            }
        }
    }
})

context(Database)
fun AbonnementExposedRepository.getAbonnement(fødselsnummer: Fødselsnummer, clientId: ClientId): Abonnement? {
    return transaction(this@Database) {
        selectAll()
            .where {
                (fødselsnummerColumn eq fødselsnummer.value) and (clientIdColumn eq clientId.value)
            }.map { AbonnementDto.fromRow(it).toDomain() }
            .firstOrNull()
    }
}
