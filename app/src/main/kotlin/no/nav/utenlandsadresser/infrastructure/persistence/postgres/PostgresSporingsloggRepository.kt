package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import no.nav.utenlandsadresser.app.SporingsloggRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.SporingsloggDto
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class PostgresSporingsloggRepository(
    val database: R2dbcDatabase,
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
        suspendTransaction(db = database, readOnly = false) {
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
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .andWhere { mottakerColumn eq organisasjonsnummer.value }
                .map { it[utlevertDataColumn] }
                .toList()
        }

    override suspend fun deleteSporingsloggerOlderThan(duration: Duration) {
        logger.info("Deleting sporingslogg older than $duration")
        suspendTransaction(db = database, readOnly = false) {
            val rowsDeleted =
                deleteWhere {
                    tidspunktForUtleveringColumn less Clock.System.now().minus(duration)
                }

            logger.info("Deleted $rowsDeleted rows from sporingslogg")
        }
    }
}
