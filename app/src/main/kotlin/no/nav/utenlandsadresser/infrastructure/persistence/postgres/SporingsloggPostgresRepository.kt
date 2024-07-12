package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.app.Sporingslogg
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.SporingsloggDto
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration

class SporingsloggPostgresRepository(
    val database: Database,
) : Table("sporingslogg"),
    Sporingslogg {
    private val jsonConfig = Json { prettyPrint = true }

    private val idColumn: Column<Int> = integer("id").autoIncrement()
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val mottakerColumn: Column<String> = text("mottaker")
    private val utlevertDataColumn: Column<SporingsloggDto> = jsonb<SporingsloggDto>("utlevert_data", jsonConfig)
    val tidspunktForUtleveringColumn: Column<Instant> = timestamp("tidspunkt_for_utlevering")

    override val primaryKey = PrimaryKey(idColumn)

    override suspend fun loggPostadresse(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        postadresse: Postadresse.Utenlandsk,
        tidspunktForUtlevering: Instant,
    ) {
        newSuspendedTransaction(Dispatchers.IO, database) {
            insert {
                it[identitetsnummerColumn] = identitetsnummer.value
                it[mottakerColumn] = organisasjonsnummer.value
                it[utlevertDataColumn] = SporingsloggDto.SporingsloggPostadresse.fromDomain(postadresse)
                it[tidspunktForUtleveringColumn] = tidspunktForUtlevering
            }
        }
    }

    suspend fun getSporingslogger(identitetsnummer: Identitetsnummer): List<SporingsloggDto> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .map { it[utlevertDataColumn] }
        }

    suspend fun deleteSporingsloggerOlderThan(duration: Duration) {
        newSuspendedTransaction(Dispatchers.IO, database) {
            deleteWhere {
                tidspunktForUtleveringColumn less Clock.System.now().minus(duration)
            }
        }
    }
}
