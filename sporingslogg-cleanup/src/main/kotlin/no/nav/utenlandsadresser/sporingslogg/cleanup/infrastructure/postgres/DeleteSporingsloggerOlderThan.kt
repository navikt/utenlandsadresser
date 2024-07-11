package no.nav.utenlandsadresser.sporingslogg.cleanup.infrastructure.postgres

import kotlinx.datetime.Clock
import no.nav.utenlandsadresser.infrastructure.persistence.postgres.SporingsloggPostgresRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration

fun SporingsloggPostgresRepository.deleteSporingsloggerOlderThan(duration: Duration) {
    transaction(database) {
        deleteWhere {
            tidspunktForUtleveringColumn less Clock.System.now().minus(duration)
        }
    }
}
