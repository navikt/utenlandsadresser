package no.nav.utenlandsadresser.database.exposed

import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.database.AbonnementRepository
import no.nav.utenlandsadresser.database.dto.AbonnementDto
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Fødselsnummer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

class AbonnementExposedRepository(
    private val database: Database,
) : AbonnementRepository, Table("abonnement") {
    val clientIdColumn: Column<String> = text("client_id")
    val fødselsnummerColumn: Column<String> = text("fødselsnummer")
    val løpenummerColumn: Column<Int> = integer("løpenummer")
    val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(fødselsnummerColumn, clientIdColumn)

    override fun createAbonnement(abonnement: Abonnement) {
        createAbonnement(AbonnementDto.fromDomain(abonnement))
    }

    override fun deleteAbonnement(fødselsnummer: Fødselsnummer, clientId: ClientId) {
        transaction(database) {
            deleteWhere {
                (fødselsnummerColumn eq fødselsnummer.value) and (clientIdColumn eq clientId.value)
            }
        }
    }

    private fun createAbonnement(abonnement: AbonnementDto) {
        transaction(database) {
            insert {
                it[fødselsnummerColumn] = abonnement.fødselsnummer
                it[clientIdColumn] = abonnement.clientId
                it[løpenummerColumn] = abonnement.løpenummer
                it[opprettetColumn] = abonnement.opprettet
            }
        }
    }
}
