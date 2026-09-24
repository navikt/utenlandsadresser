package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.utenlandsadresser.infrastructure.route.json.FeedRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.PersondataFeedResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.PersondataHendelsestypeJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StoppAbonnementJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskPostadresseJson

/**
 * POC: Persondata (v2-poc), hvor utenlandsk id legges til som ekstra data på samme feed og
 * samme abonnement som postadresse - i motsetning til [configureUtenlandskIdRoute], som er et
 * eget, separat abonnement. Endepunktene er en POC og svarer med faste dummy-verdier i stedet
 * for å gå via [no.nav.utenlandsadresser.app.AbonnementService]/[no.nav.utenlandsadresser.app.FeedService].
 * Denne POC-en gjør ingen endringer i den eksisterende v1-APIen for postadresse.
 */
@OptIn(ExperimentalKtorApi::class)
fun Route.configurePersondataRoute() {
    authenticate("postadresse-abonnement-maskinporten") {
        route("/api/v2/persondata") {
            route("/abonnement") {
                /**
                 * Start abonnement
                 *
                 * Description: Start abonnement for en person med et gitt identitetsnummer. Om personen har en utenlandsk adresse og/eller utenlandsk id ved start av abonnementet, vil den respektive dataen bli lagt på feeden. Eventuelle split og merge i Folkeregisteret på brukere som det er satt opp abonnement på må håndteres av Skatteetaten ved at man avslutter gjeldende abonnement og oppretter nytt abonnement.
                 *
                 * Responses:
                 *  - 201 Abonnementet er opprettet.
                 *  - 200 Abonnementet på gitt identitetsnummer eksisterer allerede.
                 *  - 400 Feil i forespørsel.
                 *  - 401 Manglende eller ugyldig Maskinporten-token.
                 *  - 500 Intern feil, abonnement ble ikke opprettet.
                 */
                post("/start") {
                    call.receive<StartAbonnementRequestJson>()

                    call.respond(
                        HttpStatusCode.Created,
                        StartAbonnementResponseJson(abonnementId = "00000000-0000-0000-0000-000000000000"),
                    )
                }.describe {
                    startAbonnementExamples()
                }

                /**
                 * Stopp abonnement
                 *
                 * Description: Stopp abonnement med en gitt referanse.
                 *
                 * Responses:
                 *  - 200 Abonnementet ble stoppet eller finnes ikke.
                 *  - 401 Manglende eller ugyldig Maskinporten-token.
                 */
                post("/stopp") {
                    call.receive<StoppAbonnementJson>()

                    call.respond(HttpStatusCode.OK)
                }.describe { stoppAbonnementExamples() }
            }

            /**
             * Hent neste persondata
             *
             * Description: Hent neste persondata-hendelse fra feeden. Vi skiller mellom flere typer hendelser. OPPDATERT_ADRESSE betyr at det har skjedd en endring på en persons adresse. SLETTET_ADRESSE betyr at en persons adresse er slettet. Dette skjer i utgangspunktet ved adressebeskyttelse. Om man leser en event med denne hendelsestypen så forventes det at konsumenten sletter postadressen til personen. OPPDATERT_UTENLANDSK_ID beskriver endringer i utenlandske identitetsnumre. Returnerer alltid alle tilgjengelige data, så en hendelse med f.eks. OPPDATERT_ADRESSE vil også inneholde gjeldende utenlandsk id. Merk: Endepunktet er en POC under utvikling og returnerer foreløpig faste eksempeldata.
             *
             * Responses:
             *  - 200 Persondata (postadresse og utenlandsk id). Foreløpig faste eksempeldata.
             *  - 401 Manglende eller ugyldig Maskinporten-token.
             */
            post("/feed") {
                call.receive<FeedRequestJson>()

                call.respond(
                    HttpStatusCode.OK,
                    PersondataFeedResponseJson(
                        abonnementId = "uuid",
                        identitetsnummer = "12342012345",
                        utenlandskPostadresse =
                            UtenlandskPostadresseJson(
                                adresselinje1 = "Adresselinje 1",
                                adresselinje2 = "Adresselinje 2",
                                adresselinje3 = "Adresselinje 3",
                                postnummer = "1234",
                                poststed = "Poststed",
                                landkode = "SE",
                                land = "Sverige",
                            ),
                        utenlandskId = listOf(UtenlandskIdResponseJson("123010190B456", "DEU", "Dolly")),
                        hendelsestype = PersondataHendelsestypeJson.OPPDATERT_UTENLANDSK_ID,
                    ),
                )
            }.describe {
                persondataFeedExamples()
            }
        }.describe {
            tag("poc-2")
        }
    }
}
