package no.nav.utenlandsadresser.setup

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig

/**
 * Laster inn konfigurasjon fra filer under main/resources. Se for eksempel `src/main/resources/application.conf`.
 * Miljøvariabler er referert i filene og lastes også inn her.
 *
 * Konfigurasjonen i application.conf er satt opp for prod. For kjøring lokalt og i dev så lastes det inn komplementerende
 * konfigurasjon fra egne .conf-filer.
 */
@OptIn(ExperimentalHoplite::class)
fun loadConfiguration(appEnv: AppEnv): UtenlandsadresserConfig {
    val resourceFiles =
        listOfNotNull(
            when (appEnv) {
                AppEnv.LOCAL -> "/application-local.conf"
                AppEnv.DEV_GCP -> "/application-dev-gcp.conf"
                AppEnv.PROD_GCP -> null
            },
            "/application.conf",
        )
    return ConfigLoaderBuilder
        .default()
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<UtenlandsadresserConfig>(resourceFiles)
}
