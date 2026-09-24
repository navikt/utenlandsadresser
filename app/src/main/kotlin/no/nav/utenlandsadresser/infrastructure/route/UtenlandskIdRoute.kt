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
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StoppAbonnementJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdFeedResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdHendelsestypeJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdResponseJson

/**
 * POC: Abonnement på utenlandsk ID.
 *
 * Dette er et separat abonnement fra postadresse-abonnementet. Endepunktene er foreløpig en
 * POC og svarer med faste dummy-verdier i stedet for å gå via [no.nav.utenlandsadresser.app.AbonnementService].
 * En reell implementasjon vil kreve at abonnementer på utenlandsk ID og postadresse skilles fra
 * hverandre i lagringen (f.eks. egen tabell/type for abonnement).
 */
@OptIn(ExperimentalKtorApi::class)
fun Route.configureUtenlandskIdRoute() {
    authenticate("postadresse-abonnement-maskinporten") {
        route("/api/v1/utenlandskid") {
            route("/abonnement") {
                /**
                 * Start abonnement
                 *
                 * Description: Start abonnement for en person med et gitt identitetsnummer. Om personen har en utenlandsk id ved start av abonnementet, vil denne bli lagt på feeden. Eventuelle split og merge i Folkeregisteret på brukere som det er satt opp abonnement på må håndteres av Skatteetaten ved at man avslutter gjeldende abonnement og oppretter nytt abonnement.
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
             * Hent neste utenlandsk id
             *
             * Description: Hent neste utenlandsk id-hendelse fra feeden. OPPDATERT_UTENLANDSK_ID beskriver endringer i utenlandske identitetsnumre. Merk: Endepunktet er en POC under utvikling og returnerer foreløpig faste eksempeldata.
             *
             * Responses:
             *  - 200 Utenlandsk id. Foreløpig faste eksempeldata.
             *  - 401 Manglende eller ugyldig Maskinporten-token.
             */
            post("/feed") {
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
            tag("poc-1")
        }
    }
}
