package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.AbonnementRepository
import no.nav.utenlandsadresser.infrastructure.persistence.CreateAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.DeleteAbonnementError
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.AbonnementDto
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.AbonnementDto.Companion.fromRow
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PostgresAbonnementRepository(
    private val database: R2dbcDatabase,
) : Table("abonnement"),
    AbonnementRepository {
    val idColumn: Column<Uuid> = uuid("id")
    val organisasjonsnummerColumn: Column<String> = text("organisasjonsnummer")
    val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(idColumn)

    override suspend fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Abonnement> =
        suspendTransaction(database, readOnly = false) {
            createAbonnement(AbonnementDto.fromDomain(abonnement))
        }

    override suspend fun deleteAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<DeleteAbonnementError, Unit> =
        either {
            val deletedRows =
                suspendTransaction(db = database, readOnly = false) {
                    deleteWhere {
                        (idColumn eq abonnementId) and (organisasjonsnummerColumn eq organisasjonsnummer.value)
                    }
                }

            if (deletedRows == 0) {
                raise(DeleteAbonnementError.NotFound)
            }
        }

    override suspend fun getAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> =
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .map { fromRow(it).toDomain() }
                .toList()
        }

    suspend fun getAbonnementer(identitetsnummer: List<Identitetsnummer>): List<Abonnement> =
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where { identitetsnummerColumn inList identitetsnummer.map(Identitetsnummer::value) }
                .map { fromRow(it).toDomain() }
                .toList()
        }

    private suspend fun createAbonnement(abonnement: AbonnementDto): Either<CreateAbonnementError, Abonnement> =
        suspendTransaction(db = database, readOnly = false) {
            either {
                val existingAbonnement =
                    selectAll()
                        .where { identitetsnummerColumn eq abonnement.identitetsnummer }
                        .andWhere { organisasjonsnummerColumn eq abonnement.organisasjonsnummer }
                        .map { fromRow(it).toDomain() }
                        .firstOrNull()

                if (existingAbonnement != null) {
                    raise(CreateAbonnementError.AlreadyExists(existingAbonnement))
                }

                val insertStatement =
                    insert {
                        it[idColumn] = abonnement.id
                        it[identitetsnummerColumn] = abonnement.identitetsnummer
                        it[organisasjonsnummerColumn] = abonnement.organisasjonsnummer
                        it[opprettetColumn] = abonnement.opprettet
                    }

                insertStatement.resultedValues!!.first().let {
                    fromRow(it).toDomain()
                }
            }
        }
}
