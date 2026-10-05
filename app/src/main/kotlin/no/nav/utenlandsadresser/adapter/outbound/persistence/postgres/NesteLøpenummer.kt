package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import kotlinx.coroutines.flow.firstOrNull
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select

/**
 * Finner neste løpenummer for mottakeren i en feed-tabell.
 *
 * Transaksjonsnivået er READ COMMITTED, så to samtidige transaksjoner kan lese samme høyeste løpenummer.
 * En advisory lock per tabell og mottaker serialiserer tildelingen. Låsen slippes når den ytterste transaksjonen
 * committes eller rulles tilbake, så kall denne sent i transaksjonen og sett inn raden i samme transaksjon.
 * Primærnøkkelen (organisasjonsnummer, løpenummer) er sikkerhetsnettet om låsen likevel ikke holder.
 */
suspend fun R2dbcTransaction.nesteLøpenummer(
    table: Table,
    organisasjonsnummerColumn: Column<String>,
    løpenummerColumn: Column<Int>,
    organisasjonsnummer: Organisasjonsnummer,
): Løpenummer {
    exec(
        "SELECT pg_advisory_xact_lock(hashtext(?))",
        listOf(TextColumnType() to "${table.tableName}:${organisasjonsnummer.value}"),
    )

    val høyeste =
        table
            .select(løpenummerColumn)
            .where { organisasjonsnummerColumn eq organisasjonsnummer.value }
            .orderBy(løpenummerColumn to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.get(løpenummerColumn)
            ?: 0

    return Løpenummer(høyeste + 1)
}
