package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.utenlandsadresser.application.port.outbound.CreateAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.DeleteAbonnementError
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdAbonnementRepository
import no.nav.utenlandsadresser.domain.Abonnement
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PostgresUtenlandskIdAbonnementRepository(
    private val database: R2dbcDatabase,
) : Table("utenlandsk_id_abonnement"),
    UtenlandskIdAbonnementRepository {
    private val idColumn: Column<Uuid> = uuid("id")
    private val organisasjonsnummerColumn: Column<String> = text("organisasjonsnummer")
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(idColumn)

    /**
     * Oppretter abonnementet, eller returnerer det eksisterende abonnementet for samme person og mottaker.
     */
    suspend fun createAbonnement(abonnement: Abonnement): Either<CreateAbonnementError, Abonnement> =
        suspendTransaction(db = database, readOnly = false) {
            either {
                val existingAbonnement =
                    selectAll()
                        .where { identitetsnummerColumn eq abonnement.identitetsnummer.value }
                        .andWhere { organisasjonsnummerColumn eq abonnement.organisasjonsnummer.value }
                        .map { it.toAbonnement() }
                        .firstOrNull()

                if (existingAbonnement != null) {
                    raise(CreateAbonnementError.AlreadyExists(existingAbonnement))
                }

                insert {
                    it[idColumn] = abonnement.id
                    it[identitetsnummerColumn] = abonnement.identitetsnummer.value
                    it[organisasjonsnummerColumn] = abonnement.organisasjonsnummer.value
                    it[opprettetColumn] = abonnement.opprettet
                }.resultedValues!!
                    .first()
                    .toAbonnement()
            }
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

    suspend fun getAbonnementer(identitetsnummer: Identitetsnummer): List<Abonnement> =
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .map { it.toAbonnement() }
                .toList()
        }

    private fun ResultRow.toAbonnement(): Abonnement =
        Abonnement(
            id = this[idColumn],
            organisasjonsnummer = Organisasjonsnummer(this[organisasjonsnummerColumn]),
            identitetsnummer = Identitetsnummer(this[identitetsnummerColumn]),
            opprettet = this[opprettetColumn],
        )
}
