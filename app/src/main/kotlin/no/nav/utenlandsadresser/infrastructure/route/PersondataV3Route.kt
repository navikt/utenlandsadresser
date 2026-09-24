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
import no.nav.utenlandsadresser.infrastructure.route.json.PostadresseFeedResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.PostadresseHendelsestypeJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StoppAbonnementJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdFeedResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdHendelsestypeJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskPostadresseJson

/**
 * POC: Ett felles abonnement for persondata (v3-poc), med separate feeder per datatype -
 * i motsetning til [configureUtenlandskIdRoute] (helt separat abonnement per datatype) og
 * [configurePersondataRoute] (ett abonnement, én kombinert feed). Her deler postadresse og
 * utenlandsk id samme abonnement/livssyklus, men har hver sin feed med egen løpenummer-sekvens
 * og egen, uendret hendelsestype ([PostadresseHendelsestypeJson]/[UtenlandskIdHendelsestypeJson]).
 * Endepunktene er en POC og svarer med faste dummy-verdier i stedet for å gå via
 * [no.nav.utenlandsadresser.app.AbonnementService]/[no.nav.utenlandsadresser.app.FeedService].
 * Denne POC-en gjør ingen endringer i den eksisterende v1-APIen for postadresse.
 */
@OptIn(ExperimentalKtorApi::class)
fun Route.configurePersondataV3Route() {
    authenticate("postadresse-abonnement-maskinporten") {
        route("/api/v3/persondata") {
            route("/abonnement") {
                /**
                 * Start abonnement
                 *
                 * Description: Start ett felles abonnement for persondata for en person med et gitt identitetsnummer. Abonnementet gir tilgang til både postadresse-feeden og utenlandsk id-feeden. Om personen har en utenlandsk adresse og/eller utenlandsk id ved start av abonnementet, vil den respektive dataen bli lagt på sin respektive feed. Eventuelle split og merge i Folkeregisteret på brukere som det er satt opp abonnement på må håndteres av Skatteetaten ved at man avslutter gjeldende abonnement og oppretter nytt abonnement.
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
                 * Description: Stopp abonnementet med en gitt referanse. Stopper begge feedene samtidig, siden de deler abonnement.
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
             * Hent neste postadresse
             *
             * Description: Hent neste postadresse-hendelse fra postadresse-feeden. Denne feeden har sin egen løpenummer-sekvens, uavhengig av utenlandsk id-feeden, men deler abonnement med den. OPPDATERT_ADRESSE betyr at det har skjedd en endring på en persons adresse. SLETTET_ADRESSE betyr at en persons adresse er slettet. Dette skjer i utgangspunktet ved adressebeskyttelse. Om man leser en event med denne hendelsestypen så forventes det at konsumenten sletter postadressen til personen. Merk: Endepunktet er en POC under utvikling og returnerer foreløpig faste eksempeldata.
             *
             * Responses:
             *  - 200 Postadresse. Foreløpig faste eksempeldata.
             *  - 401 Manglende eller ugyldig Maskinporten-token.
             */
            post("/postadresse/feed") {
                call.receive<FeedRequestJson>()

                call.respond(
                    HttpStatusCode.OK,
                    PostadresseFeedResponseJson(
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
                        hendelsestype = PostadresseHendelsestypeJson.OPPDATERT_ADRESSE,
                    ),
                )
            }.describe {
                feedExamples()
            }

            /**
             * Hent neste utenlandsk id
             *
             * Description: Hent neste utenlandsk id-hendelse fra utenlandsk id-feeden. Denne feeden har sin egen løpenummer-sekvens, uavhengig av postadresse-feeden, men deler abonnement med den. OPPDATERT_UTENLANDSK_ID beskriver endringer i utenlandske identitetsnumre. Merk: Endepunktet er en POC under utvikling og returnerer foreløpig faste eksempeldata.
             *
             * Responses:
             *  - 200 Utenlandsk id. Foreløpig faste eksempeldata.
             *  - 401 Manglende eller ugyldig Maskinporten-token.
             */
            post("/utenlandskid/feed") {
                call.receive<FeedRequestJson>()

                call.respond(
                    HttpStatusCode.OK,
                    UtenlandskIdFeedResponseJson(
                        abonnementId = "uuid",
                        identitetsnummer = "12342012345",
                        utenlandskId = listOf(UtenlandskIdResponseJson("123010190B456", "DEU", "Dolly")),
                        hendelsestype = UtenlandskIdHendelsestypeJson.OPPDATERT_UTENLANDSK_ID,
                    ),
                )
            }.describe {
                utenlandskIdFeedExamples()
            }
        }.describe {
            tag("poc-3")
        }
    }
}
