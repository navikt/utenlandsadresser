package no.nav.utenlandsadresser.infrastructure.persistence.exposed

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.CreateAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.DeleteAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.exposed.dto.AbonnementDto
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction

class AbonnementPostgresRepository(
    private val database: Database,
) : AbonnementRepository, Table("abonnement") {
    val organisasjonsnummerColumn: Column<String> = text("organisasjonsnummer")
    val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(identitetsnummerColumn, organisasjonsnummerColumn)

    override suspend fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Unit> {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            createAbonnement(AbonnementDto.fromDomain(abonnement))
        }
    }

    override suspend fun deleteAbonnement(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer
    ): Either<DeleteAbonnementError, Unit> = either {
        val deletedRows = newSuspendedTransaction(Dispatchers.IO, database) {
            deleteWhere {
                (identitetsnummerColumn eq identitetsnummer.value) and (organisasjonsnummerColumn eq organisasjonsnummer.value)
            }
        }

        if (deletedRows == 0) {
            raise(DeleteAbonnementError.NotFound)
        }
    }

    override suspend fun getAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .map { AbonnementDto.fromRow(it).toDomain() }
        }

    suspend fun Transaction.createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Unit> {
        return createAbonnement(AbonnementDto.fromDomain(abonnement))
    }

    private suspend fun Transaction.createAbonnement(abonnement: AbonnementDto): Either<CreateAbonnementError, Unit> {
        return either {
            try {
                withSuspendTransaction(Dispatchers.IO) {
                    insert {
                        it[identitetsnummerColumn] = abonnement.fødselsnummer
                        it[organisasjonsnummerColumn] = abonnement.organisasjonsnummer
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
