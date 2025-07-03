package no.nav.utenlandsadresser.setup

import com.zaxxer.hikari.HikariDataSource
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.hikariConfig
import no.nav.utenlandsadresser.local.startLocalPostgresContainer

context(appEnv: AppEnv, config: UtenlandsadresserConfig)
fun setupDataSource(): HikariDataSource {
    val utenlandsadresserDatabaseConfig =
        when (appEnv) {
            AppEnv.LOCAL -> startLocalPostgresContainer()
            AppEnv.DEV_GCP,
            AppEnv.PROD_GCP,
            -> config.utenlandsadresserDatabase
        }

    val hikariConfig = hikariConfig(utenlandsadresserDatabaseConfig)
    return HikariDataSource(hikariConfig)
}
