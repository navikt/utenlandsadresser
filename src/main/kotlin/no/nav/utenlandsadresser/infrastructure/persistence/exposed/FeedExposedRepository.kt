package no.nav.utenlandsadresser.infrastructure.persistence.exposed

import arrow.core.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.domain.*
import no.nav.utenlandsadresser.infrastructure.persistence.FeedRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FeedExposedRepository(
    private val database: Database,
) : FeedRepository, Table("feed") {
    private val organisasjonsnummerColumn: Column<String> = text("organisasjonsnummer")
    private val løpenummerColumn: Column<Int> = integer("løpenummer")
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(identitetsnummerColumn, løpenummerColumn, organisasjonsnummerColumn)

    override suspend fun createFeedEvent(feedEvent: FeedEvent.Incoming) {
        newSuspendedTransaction(Dispatchers.IO, database) {
            val løpenummer = (getHighestLøpenummer(feedEvent.organisasjonsnummer)?.value ?: 0) + 1
            insert {
                it[identitetsnummerColumn] = feedEvent.identitetsnummer.value
                it[organisasjonsnummerColumn] = feedEvent.organisasjonsnummer.value
                it[løpenummerColumn] = løpenummer
                it[opprettetColumn] = Clock.System.now()
            }
        }
    }

    override suspend fun getFeedEvent(
        organisasjonsnummer: Organisasjonsnummer,
        løpenummer: Løpenummer
    ): FeedEvent.Outgoing? {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            selectAll()
                .where {
                    (organisasjonsnummerColumn eq organisasjonsnummer.value) and (løpenummerColumn eq løpenummer.value)
                }.firstOrNull()
                ?.let {
                    FeedEvent.Outgoing(
                        identitetsnummer = Identitetsnummer(it[identitetsnummerColumn])
                            .getOrElse { throw IllegalStateException("Invalid identitetsnummer") },
                        organisasjonsnummer = Organisasjonsnummer(it[organisasjonsnummerColumn]),
                        løpenummer = Løpenummer(it[løpenummerColumn]),
                    )
                }
        }
    }

    private suspend fun getHighestLøpenummer(organisasjonsnummer: Organisasjonsnummer): Løpenummer? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            select(løpenummerColumn)
                .where { organisasjonsnummerColumn eq organisasjonsnummer.value }
                .orderBy(løpenummerColumn to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { Løpenummer(it[løpenummerColumn]) }
        }
}