package no.nav.utenlandsadresser.hent.utenlandsadresser

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.PdlMottakHttpClient
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.UtenlandskAdresseJson
import no.nav.utenlandsadresser.hent.utenlandsadresser.config.HentUtenlandsadresserConfig
import no.nav.utenlandsadresser.infrastructure.client.http.configureAuthHttpClient
import java.net.URI
import java.util.*

@OptIn(ExperimentalHoplite::class)
suspend fun main() {
    val appEnv = AppEnv.getFromEnvVariable("APP_ENV")
    configureLogging(appEnv)

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

    val config: HentUtenlandsadresserConfig =
        ConfigLoaderBuilder
            .default()
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow(resourceFiles)

    val pdlMottakHttpClient =
        configureAuthHttpClient(
            config.oAuth,
            listOf(Scope(config.pdlMottak.scope)),
        )

    val oppdaterUtenlandsadresseClient =
        PdlMottakHttpClient(pdlMottakHttpClient, URI.create(config.pdlMottak.baseUrl).toURL())

    oppdaterUtenlandsadresseClient.oppdaterUtenlandsadresse(
        Identitetsnummer("24909098307").value,
        UtenlandskAdresseJson.MedAdressenavnNummer(
            adressenavnNummer = "Testgate ${(1..100).random()}",
            bygningEtasjeLeilighet = "Etasje ${(1..10).random()}",
            postkode = (1..9999).random().toString(),
            bySted = "Utlandsby",
            regionDistriktOmraade = "Utlandsregion",
            landkode = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA3).random(),
        ),
    )
}
