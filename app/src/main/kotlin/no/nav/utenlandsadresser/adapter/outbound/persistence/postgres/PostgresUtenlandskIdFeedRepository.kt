package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import kotlinx.coroutines.flow.firstOrNull
import no.nav.utenlandsadresser.application.port.outbound.UtenlandskIdFeedRepository
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.UtenlandskIdFeedEvent
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PostgresUtenlandskIdFeedRepository(
    private val database: R2dbcDatabase,
    private val clock: Clock,
) : Table("utenlandsk_id_feed"),
    UtenlandskIdFeedRepository {
    private val organisasjonsnummerColumn: Column<String> = text("organisasjonsnummer")
    private val løpenummerColumn: Column<Int> = integer("løpenummer")
    private val identitetsnummerColumn: Column<String> = text("identitetsnummer")
    private val abonnementIdColumn: Column<Uuid> = uuid("abonnement_id")
    private val hendelsestypeColumn: Column<UtenlandskIdHendelsestypePostgres> = enumeration("hendelsestype")
    private val opprettetColumn: Column<Instant> = timestamp("opprettet")

    override val primaryKey = PrimaryKey(organisasjonsnummerColumn, løpenummerColumn)

    override suspend fun getFeedEvent(
        organisasjonsnummer: Organisasjonsnummer,
        løpenummer: Løpenummer,
    ): UtenlandskIdFeedEvent.Outgoing? =
        suspendTransaction(db = database, readOnly = true) {
            selectAll()
                .where {
                    (organisasjonsnummerColumn eq organisasjonsnummer.value) and (løpenummerColumn eq løpenummer.value)
                }.firstOrNull()
                ?.let {
                    UtenlandskIdFeedEvent.Outgoing(
                        identitetsnummer = Identitetsnummer(it[identitetsnummerColumn]),
                        abonnementId = it[abonnementIdColumn],
                        hendelsestype = it[hendelsestypeColumn].toDomain(),
                    )
                }
        }

    /**
     * Legger hendelsen på feeden med neste løpenummer for mottakeren. Se [nesteLøpenummer] for låsingen.
     */
    suspend fun createFeedEvent(
        feedEvent: UtenlandskIdFeedEvent.Incoming,
        timestamp: Instant = clock.now(),
    ) {
        suspendTransaction(db = database, readOnly = false) {
            val løpenummer =
                nesteLøpenummer(
                    table = this@PostgresUtenlandskIdFeedRepository,
                    organisasjonsnummerColumn = organisasjonsnummerColumn,
                    løpenummerColumn = løpenummerColumn,
                    organisasjonsnummer = feedEvent.organisasjonsnummer,
                )

            insert {
                it[organisasjonsnummerColumn] = feedEvent.organisasjonsnummer.value
                it[løpenummerColumn] = løpenummer.value
                it[identitetsnummerColumn] = feedEvent.identitetsnummer.value
                it[abonnementIdColumn] = feedEvent.abonnementId
                it[hendelsestypeColumn] = UtenlandskIdHendelsestypePostgres.fromDomain(feedEvent.hendelsestype)
                it[opprettetColumn] = timestamp
            }
        }
    }
}
