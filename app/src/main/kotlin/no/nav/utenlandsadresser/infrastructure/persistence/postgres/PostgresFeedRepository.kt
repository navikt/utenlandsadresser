package no.nav.utenlandsadresser.infrastructure.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.persistence.FeedRepository
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import java.util.*
import kotlin.time.Duration

class PostgresFeedRepository(
    private val database: Database,
) : Table("feed"),
    FeedRepository {
    private val organisasjonsnummerColumn: Column<String> = text("organisasjonsnummer")
    private val løpenummerColumn: Column<Int> = integer("løpenummer")
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val abonnementIdColumn: Column<UUID> = uuid("abonnement_id")
    private val hendelsestypeColumn: Column<HendelsestypePostgres> = enumeration("hendelsestype")
    private val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(identitetsnummerColumn, løpenummerColumn, organisasjonsnummerColumn)

    override suspend fun getFeedEvent(
        organisasjonsnummer: Organisasjonsnummer,
        løpenummer: Løpenummer,
    ): FeedEvent.Outgoing? =
        newSuspendedTransaction(Dispatchers.IO, database) {
            selectAll()
                .where {
                    (organisasjonsnummerColumn eq organisasjonsnummer.value) and (løpenummerColumn eq løpenummer.value)
                }.firstOrNull()
                ?.let {
                    FeedEvent.Outgoing(
                        identitetsnummer = Identitetsnummer(it[identitetsnummerColumn]),
                        abonnementId = it[abonnementIdColumn],
                        hendelsestype = it[hendelsestypeColumn].toDomain(),
                    )
                }
        }

    suspend fun Transaction.hasEventBeenAddedInTheLast(
        duration: Duration,
        identitetsnummer: Identitetsnummer,
        abonnementId: UUID,
        hendelsestype: Hendelsestype,
    ): Boolean =
        !withSuspendTransaction {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .andWhere { abonnementIdColumn eq abonnementId }
                .andWhere { opprettetColumn greaterEq Clock.System.now().minus(duration) }
                .andWhere { hendelsestypeColumn eq HendelsestypePostgres.fromDomain(hendelsestype) }
                .empty()
        }

    suspend fun Transaction.createFeedEvent(
        feedEvent: FeedEvent.Incoming,
        timestamp: Instant = Clock.System.now(),
    ) {
        withSuspendTransaction {
            val løpenummer = (getHighestLøpenummer(feedEvent.organisasjonsnummer)?.value ?: 0) + 1
            insert {
                it[identitetsnummerColumn] = feedEvent.identitetsnummer.value
                it[abonnementIdColumn] = feedEvent.abonnementId
                it[organisasjonsnummerColumn] = feedEvent.organisasjonsnummer.value
                it[løpenummerColumn] = løpenummer
                it[hendelsestypeColumn] = HendelsestypePostgres.fromDomain(feedEvent.hendelsestype)
                it[opprettetColumn] = timestamp
            }
        }
    }

    private suspend fun Transaction.getHighestLøpenummer(organisasjonsnummer: Organisasjonsnummer): Løpenummer? =
        withSuspendTransaction {
            select(løpenummerColumn)
                .where { organisasjonsnummerColumn eq organisasjonsnummer.value }
                .orderBy(løpenummerColumn to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { Løpenummer(it[løpenummerColumn]) }
        }
}
