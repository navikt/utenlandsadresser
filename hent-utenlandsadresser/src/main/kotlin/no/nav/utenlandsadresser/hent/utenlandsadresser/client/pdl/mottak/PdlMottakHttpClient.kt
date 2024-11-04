package no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import no.nav.utenlandsadresser.hent.utenlandsadresser.OppdaterUtenlandsadresse
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.EndringsmeldingJson
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.EndringstypeJson
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.OpplysningstypeJson
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.PersonendringJson
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.PersonopplysningJson
import no.nav.utenlandsadresser.hent.utenlandsadresser.client.pdl.mottak.json.UtenlandskAdresseJson
import java.net.URL

class PdlMottakHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: URL,
) : OppdaterUtenlandsadresse {
    override suspend fun oppdaterUtenlandsadresse(
        identitetsnummer: String,
        utenlandskAdresse: UtenlandskAdresseJson,
    ) {
        httpClient.post(baseUrl) {
            contentType(ContentType.Application.Json)
            val iDag =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            setBody(
                PersonendringJson(
                    listOf(
                        PersonopplysningJson(
                            endringstype = EndringstypeJson.OPPRETT,
                            ident = identitetsnummer,
                            opplysningstype = OpplysningstypeJson.UTENLANDSK_KONTAKTADRESSE,
                            endringsmelding =
                                EndringsmeldingJson.Kontaktadresse(
                                    kilde = "Skatteetaten",
                                    gyldigFraOgMed = iDag,
                                    gyldigTilOgMed = iDag.plus(1, DateTimeUnit.YEAR),
                                    coAdressenavn = null,
                                    adresse = utenlandskAdresse,
                                ),
                            opplysningsId = null,
                        ),
                    ),
                ),
            )
        }
    }
}
