package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.app.Sporingslogg
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.SporingsloggDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SporingsloggPostgresRepository(
    private val database: Database,
) : Sporingslogg, Table("sporingslogg") {
    private val jsonConfig = Json { prettyPrint = true }

    private val idColumn: Column<Int> = integer("id").autoIncrement()
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val mottakerColumn: Column<String> = text("mottaker")
    private val utlevertDataColumn: Column<SporingsloggDto> = jsonb<SporingsloggDto>("utlevert_data", jsonConfig)
    private val tidspunktForUtleveringColumn: Column<Instant> = timestamp("tidspunkt_for_utlevering")

    override val primaryKey = PrimaryKey(idColumn)

    override suspend fun loggPostadresse(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        postadresse: Postadresse.Utenlandsk
    ) {
        newSuspendedTransaction(Dispatchers.IO, database) {
            insert {
                it[identitetsnummerColumn] = identitetsnummer.value
                it[mottakerColumn] = organisasjonsnummer.value
                it[utlevertDataColumn] = SporingsloggDto.SporingsloggPostadresse.fromDomain(postadresse)
            }
        }
    }

    suspend fun getSporingslogger(identitetsnummer: Identitetsnummer): List<SporingsloggDto> {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .map { it[utlevertDataColumn] }
        }
    }
}
