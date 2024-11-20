package no.nav.utenlandsadresser.hent.utenlandsadresser.setup

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.hent.utenlandsadresser.config.HentUtenlandsadresserConfig

@OptIn(ExperimentalHoplite::class)
fun loadConfiguration(appEnv: AppEnv): HentUtenlandsadresserConfig {
    val resourceFiles =
        listOfNotNull(
            when (appEnv) {
                AppEnv.LOCAL,
                -> "/hent-utenlandsadresser-local.conf"

                AppEnv.DEV_GCP,
                -> "/hent-utenlandsadresser-dev-gcp.conf"

                AppEnv.PROD_GCP,
                -> null
            },
            "/hent-utenlandsadresser.conf",
        )

    return ConfigLoaderBuilder
        .default()
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<HentUtenlandsadresserConfig>(resourceFiles)
}
