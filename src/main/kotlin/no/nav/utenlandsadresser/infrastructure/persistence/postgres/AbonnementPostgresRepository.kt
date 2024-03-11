package no.nav.utenlandsadresser.infrastructure.persistence.postgres

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
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.AbonnementDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import java.util.*

class AbonnementPostgresRepository(
    private val database: Database,
) : AbonnementRepository, Table("abonnement") {
    val idColumn: Column<UUID> = uuid("id")
    val organisasjonsnummerColumn: Column<String> = text("organisasjonsnummer")
    val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(idColumn)

    override suspend fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Abonnement> {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            createAbonnement(AbonnementDto.fromDomain(abonnement))
        }
    }

    override suspend fun deleteAbonnement(
        abonnementId: UUID,
        organisasjonsnummer: Organisasjonsnummer
    ): Either<DeleteAbonnementError, Unit> = either {
        val deletedRows = newSuspendedTransaction(Dispatchers.IO, database) {
            deleteWhere {
                (idColumn eq abonnementId) and (organisasjonsnummerColumn eq organisasjonsnummer.value)
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

    suspend fun Transaction.getAbonnementer(identitetsnummer: List<Identitetsnummer>): List<Abonnement> {
        return withSuspendTransaction {
            selectAll()
                .where { identitetsnummerColumn inList identitetsnummer.map(Identitetsnummer::value) }
                .map { AbonnementDto.fromRow(it).toDomain() }
        }
    }

    suspend fun Transaction.createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Abonnement> {
        return createAbonnement(AbonnementDto.fromDomain(abonnement))
    }

    private suspend fun Transaction.createAbonnement(abonnement: AbonnementDto): Either<CreateAbonnementError, Abonnement> {
        return withSuspendTransaction(Dispatchers.IO) {
            either {
                val existingAbonnement = selectAll()
                    .where { identitetsnummerColumn eq abonnement.identitetsnummer }
                    .andWhere { organisasjonsnummerColumn eq abonnement.organisasjonsnummer }
                    .map { AbonnementDto.fromRow(it).toDomain() }
                    .firstOrNull()

                if (existingAbonnement != null) {
                    raise(CreateAbonnementError.AlreadyExists(existingAbonnement))
                }

                val insertStatement = insert {
                    it[idColumn] = abonnement.id
                    it[identitetsnummerColumn] = abonnement.identitetsnummer
                    it[organisasjonsnummerColumn] = abonnement.organisasjonsnummer
                    it[opprettetColumn] = abonnement.opprettet
                }

                insertStatement.resultedValues!!.first().let {
                    AbonnementDto.fromRow(it).toDomain()
                }
            }
        }
    }
}

