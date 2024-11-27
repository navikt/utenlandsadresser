package no.nav.utenlandsadresser.hent.utenlandsadresser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.utenlandsadresser.AppEnv
import no.nav.utenlandsadresser.config.configureLogging
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.UtenlandsadresserHttpClient
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.PdlMottakHttpClient
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.AdresseJson
import no.nav.utenlandsadresser.hent.utenlandsadresser.config.HentUtenlandsadresserConfig
import no.nav.utenlandsadresser.hent.utenlandsadresser.setup.loadConfiguration
import no.nav.utenlandsadresser.infrastructure.client.http.createAuthHttpClient
import no.nav.utenlandsadresser.infrastructure.client.http.createHttpClient
import java.net.URI
import java.util.*

suspend fun main() {
    val appEnv = AppEnv.getFromEnvVariable("APP_ENV")
    configureLogging(appEnv)

    val config: HentUtenlandsadresserConfig = loadConfiguration(appEnv)

    val oppdaterUtenlandsadresseClient: OppdaterUtenlandsadresse =
        PdlMottakHttpClient(
            httpClient =
                createAuthHttpClient(
                    config.oAuth,
                    listOf(Scope(config.pdlMottak.scope)),
                ),
            baseUrl = URI.create(config.pdlMottak.baseUrl).toURL(),
        )
    val sporingslogger: Sporingslogg =
        UtenlandsadresserHttpClient(
            httpClient = createHttpClient(),
            baseUrl = URI.create(config.utenlandsadresser.baseUrl).toURL(),
        )

    val utenlandskAdresse =
        AdresseJson.Utenlandsk(
            adressenavnNummer = "Testgate ${(1..100).random()}",
            bygningEtasjeLeilighet = "Etasje ${(1..10).random()}",
            postboksNummerNavn = null,
            postkode = (1..9999).random().toString().padStart(4, '0'),
            bySted = "Utlandsby",
            regionDistriktOmraade = "Utlandsregion",
            landkode = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA3).random(),
        )
    val identitetsnummer = Identitetsnummer("24909098307")
    val nav = Organisasjonsnummer("889640782")

    // Logg adresser vi mottar fra Skatteetaten
    sporingslogger.logg(identitetsnummer, nav, Json.encodeToJsonElement(utenlandskAdresse))

    // Oppdater adresse i PDL
    oppdaterUtenlandsadresseClient.oppdaterUtenlandsadresse(
        identitetsnummer.value,
        utenlandskAdresse,
    )
}
