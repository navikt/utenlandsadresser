package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import kotlinx.coroutines.flow.firstOrNull
import no.nav.utenlandsadresser.application.port.outbound.FeedRepository
import no.nav.utenlandsadresser.domain.FeedEvent
import no.nav.utenlandsadresser.domain.Hendelsestype
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PostgresFeedRepository(
    private val database: R2dbcDatabase,
    private val clock: Clock,
) : Table("feed"),
    FeedRepository {
    private val organisasjonsnummerColumn: Column<String> = text("organisasjonsnummer")
    private val løpenummerColumn: Column<Int> = integer("løpenummer")
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val abonnementIdColumn: Column<Uuid> = uuid("abonnement_id")
    private val hendelsestypeColumn: Column<HendelsestypePostgres> = enumeration("hendelsestype")
    private val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(organisasjonsnummerColumn, løpenummerColumn)

    override suspend fun getFeedEvent(
        organisasjonsnummer: Organisasjonsnummer,
        løpenummer: Løpenummer,
    ): FeedEvent.Outgoing? =
        suspendTransaction(db = database, readOnly = true) {
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

    suspend fun hasEventBeenAddedInTheLast(
        duration: Duration,
        identitetsnummer: Identitetsnummer,
        abonnementId: Uuid,
        hendelsestype: Hendelsestype,
    ): Boolean =
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where { identitetsnummerColumn eq identitetsnummer.value }
                .andWhere { abonnementIdColumn eq abonnementId }
                .andWhere { opprettetColumn greaterEq clock.now().minus(duration) }
                .andWhere { hendelsestypeColumn eq HendelsestypePostgres.fromDomain(hendelsestype) }
                .empty()
                .not()
        }

    /**
     * Legger hendelsen på feeden med neste løpenummer for mottakeren. Se [nesteLøpenummer] for låsingen.
     */
    suspend fun createFeedEvent(
        feedEvent: FeedEvent.Incoming,
        timestamp: Instant = clock.now(),
    ) {
        suspendTransaction(db = database, readOnly = false) {
            val løpenummer =
                nesteLøpenummer(
                    table = this@PostgresFeedRepository,
                    organisasjonsnummerColumn = organisasjonsnummerColumn,
                    løpenummerColumn = løpenummerColumn,
                    organisasjonsnummer = feedEvent.organisasjonsnummer,
                )
            insert {
                it[identitetsnummerColumn] = feedEvent.identitetsnummer.value
                it[abonnementIdColumn] = feedEvent.abonnementId
                it[organisasjonsnummerColumn] = feedEvent.organisasjonsnummer.value
                it[løpenummerColumn] = løpenummer.value
                it[hendelsestypeColumn] = HendelsestypePostgres.fromDomain(feedEvent.hendelsestype)
                it[opprettetColumn] = timestamp
            }
        }
    }
}
