package no.nav.utenlandsadresser.infrastructure.persistence.exposed

import arrow.core.getOrElse
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.infrastructure.persistence.FeedRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

class FeedExposedRepository(
    private val database: Database,
) : FeedRepository, Table("feed") {
    private val clientIdColumn: Column<String> = text("client_id")
    private val løpenummerColumn: Column<Int> = integer("løpenummer")
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(identitetsnummerColumn, løpenummerColumn, clientIdColumn)

    override fun createFeedEvent(feedEvent: FeedEvent.Incoming) {
        transaction(database) {
            val løpenummer = (getHighestLøpenummer(feedEvent.clientId)?.value ?: 0) + 1
            insert {
                it[identitetsnummerColumn] = feedEvent.identitetsnummer.value
                it[clientIdColumn] = feedEvent.clientId.value
                it[løpenummerColumn] = løpenummer
                it[opprettetColumn] = Clock.System.now()
            }
        }
    }

    override fun getFeedEvent(clientId: ClientId, løpenummer: Løpenummer): FeedEvent.Outgoing? {
        return transaction(database) {
            selectAll()
                .where {
                    (clientIdColumn eq clientId.value) and (løpenummerColumn eq løpenummer.value)
                }.firstOrNull()
                ?.let {
                    FeedEvent.Outgoing(
                        identitetsnummer = Identitetsnummer(it[identitetsnummerColumn])
                            .getOrElse { throw IllegalStateException("Invalid identitetsnummer") },
                        clientId = ClientId(it[clientIdColumn]),
                        løpenummer = Løpenummer(it[løpenummerColumn]),
                    )
                }
        }
    }

    private fun getHighestLøpenummer(clientId: ClientId): Løpenummer? = transaction(database) {
        select(løpenummerColumn)
            .where { clientIdColumn eq clientId.value }
            .orderBy(løpenummerColumn to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.let { Løpenummer(it[løpenummerColumn]) }
    }
}