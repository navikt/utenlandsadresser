package no.nav.utenlandsadresser.setup

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig

@OptIn(ExperimentalHoplite::class)
fun loadConfiguration(appEnv: AppEnv): UtenlandsadresserConfig {
    val resourceFiles =
        listOf(
            when (appEnv) {
                AppEnv.LOCAL -> "/application-local.conf"
                AppEnv.DEV_GCP -> "/application-dev-gcp.conf"
                AppEnv.PROD_GCP -> "/application-prod-gcp.conf"
            },
            "/application.conf",
        )
    return ConfigLoaderBuilder
        .default()
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<UtenlandsadresserConfig>(resourceFiles)
}
