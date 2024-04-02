package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.app.Sporingslogg
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Postadresse
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.dto.SporingsloggDto
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

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

    override fun loggPostadresse(
        identitetsnummer: Identitetsnummer,
        organisasjonsnummer: Organisasjonsnummer,
        postadresse: Postadresse.Utenlandsk
    ) {
        transaction(database) {
            insert {
                it[identitetsnummerColumn] = identitetsnummer.value
                it[mottakerColumn] = organisasjonsnummer.value
                it[utlevertDataColumn] = SporingsloggDto.SporingsloggPostadresse.fromDomain(postadresse)
            }
        }
    }
}
