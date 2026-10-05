package no.nav.utenlandsadresser.adapter.outbound.persistence.postgres

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.utenlandsadresser.kotest.extension.migrateDatabaseTo
import no.nav.utenlandsadresser.kotest.extension.migrateDatabaseToLatest
import no.nav.utenlandsadresser.kotest.extension.setupDatabase
import no.nav.utenlandsadresser.kotest.extension.withJdbcConnection

@Isolate
class V9UniqueLøpenummerMigrationTest :
    WordSpec({
        setupDatabase()

        val skatteetaten = "974761076"
        val annenMottaker = "889640782"

        // Syntetiske identer
        val person1 = "01010100001"
        val person2 = "01010100002"
        val stoppetPerson = "01010100003"

        val abonnementSkatteetatenPerson1 = "00000000-0000-0000-0000-000000000001"
        val abonnementSkatteetatenPerson2 = "00000000-0000-0000-0000-000000000002"
        val stoppetAbonnement = "00000000-0000-0000-0000-000000000003"
        val abonnementAnnenPerson1 = "00000000-0000-0000-0000-000000000004"
        val abonnementAnnenPerson2 = "00000000-0000-0000-0000-000000000005"

        fun execute(sql: String) = withJdbcConnection { it.createStatement().use { statement -> statement.execute(sql) } }

        fun feed(organisasjonsnummer: String): List<Pair<Int, String>> =
            withJdbcConnection { connection ->
                connection
                    .prepareStatement(
                        """SELECT "løpenummer", identitetsnummer FROM feed WHERE organisasjonsnummer = ? ORDER BY "løpenummer"""",
                    ).use { statement ->
                        statement.setString(1, organisasjonsnummer)
                        statement.executeQuery().use { rs ->
                            buildList { while (rs.next()) add(rs.getInt(1) to rs.getString(2)) }
                        }
                    }
            }

        fun feedRow(
            organisasjonsnummer: String,
            løpenummer: Int,
            identitetsnummer: String,
            abonnementId: String,
        ) = "('$organisasjonsnummer', $løpenummer, '$identitetsnummer', '2026-01-01 12:00:00', '$abonnementId', 0)"

        beforeTest {
            migrateDatabaseTo("8")

            execute(
                """
                INSERT INTO abonnement (id, organisasjonsnummer, identitetsnummer, opprettet) VALUES
                ('$abonnementSkatteetatenPerson1', '$skatteetaten', '$person1', '2026-01-01 12:00:00'),
                ('$abonnementSkatteetatenPerson2', '$skatteetaten', '$person2', '2026-01-01 12:00:00'),
                ('$abonnementAnnenPerson1', '$annenMottaker', '$person1', '2026-01-01 12:00:00'),
                ('$abonnementAnnenPerson2', '$annenMottaker', '$person2', '2026-01-01 12:00:00');
                """.trimIndent(),
            )

            execute(
                """
                INSERT INTO feed (organisasjonsnummer, "løpenummer", identitetsnummer, opprettet, abonnement_id, hendelsestype) VALUES
                ${feedRow(skatteetaten, 1, person1, abonnementSkatteetatenPerson1)},
                ${feedRow(skatteetaten, 2, person2, abonnementSkatteetatenPerson2)},
                ${feedRow(skatteetaten, 2, stoppetPerson, stoppetAbonnement)},
                ${feedRow(skatteetaten, 3, person1, abonnementSkatteetatenPerson1)},
                ${feedRow(skatteetaten, 3, person2, abonnementSkatteetatenPerson2)},
                ${feedRow(annenMottaker, 1, person1, abonnementAnnenPerson1)},
                ${feedRow(annenMottaker, 1, person2, abonnementAnnenPerson2)};
                """.trimIndent(),
            )

            migrateDatabaseToLatest()
        }

        "V9" should {
            "keep one event per løpenummer and append all duplicates with an active abonnement" {
                feed(skatteetaten) shouldContainExactly
                    listOf(
                        1 to person1,
                        2 to person2,
                        3 to person1,
                        4 to person2,
                        5 to person1,
                        6 to person2,
                    )
            }

            "not copy events for stopped abonnementer" {
                feed(skatteetaten).map { it.second }.contains(stoppetPerson) shouldBe false
            }

            "count løpenummer separately per organisasjonsnummer" {
                feed(annenMottaker) shouldContainExactly
                    listOf(
                        1 to person1,
                        2 to person1,
                        3 to person2,
                    )
            }

            "make (organisasjonsnummer, løpenummer) the primary key" {
                val result =
                    runCatching {
                        execute(
                            """
                            INSERT INTO feed (organisasjonsnummer, "løpenummer", identitetsnummer, opprettet, abonnement_id, hendelsestype)
                            VALUES ${feedRow(skatteetaten, 1, person2, abonnementSkatteetatenPerson2)};
                            """.trimIndent(),
                        )
                    }

                result.isFailure shouldBe true
            }
        }
    })
