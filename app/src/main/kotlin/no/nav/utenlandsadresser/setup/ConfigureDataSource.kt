package no.nav.utenlandsadresser.setup

import com.zaxxer.hikari.HikariDataSource
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.config.UtenlandsadresserConfiguration
import no.nav.utenlandsadresser.config.createHikariConfig
import no.nav.utenlandsadresser.local.startLocalPostgresContainer

fun configureDataSource(
    appEnv: AppEnv,
    config: UtenlandsadresserConfiguration,
): HikariDataSource {
    val utenlandsadresserDatabaseConfig =
        when (appEnv) {
            AppEnv.LOCAL -> startLocalPostgresContainer()
            AppEnv.DEV_GCP,
            AppEnv.PROD_GCP,
            -> config.utenlandsadresserDatabase
        }

    val hikariConfig = createHikariConfig(utenlandsadresserDatabaseConfig)
    return HikariDataSource(hikariConfig)
}
