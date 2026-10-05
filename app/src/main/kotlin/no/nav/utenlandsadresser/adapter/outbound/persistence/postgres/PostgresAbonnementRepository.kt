package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.utenlandsadresser.adapter.outbound.persistence.postgres.AbonnementPostgres.Companion.fromRow
import no.nav.utenlandsadresser.application.port.outbound.AbonnementRepository
import no.nav.utenlandsadresser.application.port.outbound.OpprettAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.SlettAbonnementError
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
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

    override suspend fun opprettAbonnement(abonnement: Abonnement): Either<OpprettAbonnementError, Abonnement> =
        suspendTransaction(database, readOnly = false) {
            opprettAbonnement(AbonnementPostgres.fromDomain(abonnement))
        }

    override suspend fun slettAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Either<SlettAbonnementError, Unit> =
        either {
            val deletedRows =
                suspendTransaction(db = database, readOnly = false) {
                    deleteWhere {
                        (idColumn eq abonnementId) and (organisasjonsnummerColumn eq organisasjonsnummer.value)
                    }
                }

            if (deletedRows == 0) {
                raise(SlettAbonnementError.IkkeFunnet)
            }
        }

    override suspend fun hentAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> =
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .map { fromRow(it).toDomain() }
                .toList()
        }

    override suspend fun finnesAbonnement(
        abonnementId: Uuid,
        organisasjonsnummer: Organisasjonsnummer,
    ): Boolean =
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where { (idColumn eq abonnementId) and (organisasjonsnummerColumn eq organisasjonsnummer.value) }
                .empty()
                .not()
        }

    suspend fun hentAbonnementer(identitetsnummer: List<Identitetsnummer>): List<Abonnement> =
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where { identitetsnummerColumn inList identitetsnummer.map(Identitetsnummer::value) }
                .map { fromRow(it).toDomain() }
                .toList()
        }

    private suspend fun opprettAbonnement(abonnement: AbonnementPostgres): Either<OpprettAbonnementError, Abonnement> =
        suspendTransaction(db = database, readOnly = false) {
            either {
                val existingAbonnement =
                    selectAll()
                        .where { identitetsnummerColumn eq abonnement.identitetsnummer }
                        .andWhere { organisasjonsnummerColumn eq abonnement.organisasjonsnummer }
                        .map { fromRow(it).toDomain() }
                        .firstOrNull()

                if (existingAbonnement != null) {
                    raise(OpprettAbonnementError.FinnesAllerede(existingAbonnement))
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
