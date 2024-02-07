package no.nav.utenlandsadresser.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.dto.AbonnementDto
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

class AbonnementExposedRepository(
    private val database: Database,
) : AbonnementRepository, Table("abonnement") {
    val clientIdColumn: Column<String> = text("client_id")
    val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(identitetsnummerColumn, clientIdColumn)

    override fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Unit> {
        return createAbonnement(AbonnementDto.fromDomain(abonnement))
    }

    override fun deleteAbonnement(identitetsnummer: Identitetsnummer, clientId: ClientId) {
        transaction(database) {
            deleteWhere {
                (identitetsnummerColumn eq identitetsnummer.value) and (clientIdColumn eq clientId.value)
            }
        }
    }

    override fun getAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> =
        transaction(database) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .map { AbonnementDto.fromRow(it).toDomain() }
        }

    private fun createAbonnement(abonnement: AbonnementDto): Either<CreateAbonnementError, Unit> {
        return either {
            try {
                transaction(database) {
                    insert {
                        it[identitetsnummerColumn] = abonnement.fødselsnummer
                        it[clientIdColumn] = abonnement.clientId
                        it[opprettetColumn] = abonnement.opprettet
                    }
                }
            } catch (e: ExposedSQLException) {
                when (e.sqlState) {
                    // Postgres unique_violation error code
                    "23505" -> raise(CreateAbonnementError.AlreadyExists)
                    else -> throw e
                }
            }
        }
    }
}

sealed class CreateAbonnementError {
    data object AlreadyExists : CreateAbonnementError()
}
