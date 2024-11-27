package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.app.SporingsloggRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.SporingsloggDto
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import kotlin.time.Duration

class PostgresSporingsloggRepository(
    val database: Database,
) : Table("sporingslogg"),
    SporingsloggRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val jsonConfig = Json

    private val idColumn: Column<Int> = integer("id").autoIncrement()
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val mottakerColumn: Column<String> = text("mottaker")
    private val utlevertDataColumn: Column<JsonElement> = jsonb<JsonElement>("utlevert_data", jsonConfig)
    private val tidspunktForUtleveringColumn: Column<Instant> = timestamp("tidspunkt_for_utlevering")

    override val primaryKey = PrimaryKey(idColumn)

    override suspend fun loggPostadresse(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        postadresse: Postadresse.Utenlandsk,
        tidspunktForUtlevering: Instant,
    ) {
        val jsonElement = SporingsloggDto.SporingsloggPostadresse.fromDomain(postadresse).encodeToJsonElement()
        loggJson(
            identitetsnummer = identitetsnummer,
            organisasjonsnummer = organisasjonsnummer,
            json = jsonElement,
            tidspunktForUtlevering = tidspunktForUtlevering,
        )
    }

    override suspend fun loggJson(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        json: JsonElement,
        tidspunktForUtlevering: Instant,
    ) {
        newSuspendedTransaction(Dispatchers.IO, database) {
            insert {
                it[identitetsnummerColumn] = identitetsnummer.value
                it[mottakerColumn] = organisasjonsnummer.value
                it[utlevertDataColumn] = json
                it[tidspunktForUtleveringColumn] = tidspunktForUtlevering
            }
        }
    }

    suspend fun getSporingslogger(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
    ): List<JsonElement> =
        newSuspendedTransaction(Dispatchers.IO, database) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .andWhere { mottakerColumn eq organisasjonsnummer.value }
                .map { it[utlevertDataColumn] }
        }

    override suspend fun deleteSporingsloggerOlderThan(duration: Duration) {
        logger.info("Deleting sporingslogg older than $duration")
        newSuspendedTransaction(Dispatchers.IO, database) {
            val rowsDeleted =
                deleteWhere {
                    tidspunktForUtleveringColumn less Clock.System.now().minus(duration)
                }

            logger.info("Deleted $rowsDeleted rows from sporingslogg")
        }
    }
}
