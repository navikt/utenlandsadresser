package no.nav.utenlandsadresser.setup

import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.config.UtenlandsadresserDatabaseConfig
import no.nav.utenlandsadresser.local.startLocalPostgresContainer

context(appEnv: AppEnv, config: UtenlandsadresserConfig)
fun setupDatabase(): UtenlandsadresserDatabaseConfig =
    when (appEnv) {
        AppEnv.LOCAL -> startLocalPostgresContainer()

        AppEnv.DEV_GCP,
        AppEnv.PROD_GCP,
        -> config.utenlandsadresserDatabase
    }
