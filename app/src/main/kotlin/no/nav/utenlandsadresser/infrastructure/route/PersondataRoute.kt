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
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.infrastructure.route.json.FeedRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.FeedResponseV2Json
import no.nav.utenlandsadresser.infrastructure.route.json.HendelsestypeV2Json
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdResponseJson

@OptIn(ExperimentalKtorApi::class)
fun Route.configurePersondataRoute(abonnementService: AbonnementService) {
    authenticate("postadresse-abonnement-maskinporten") {
        route("/api/v2/persondata") {
            route("/abonnement") {
                /**
                 * Start abonnement
                 *
                 * Description: Start abonnement for en person med et gitt identitetsnummer. Om personen har en utenlandsk adresse og/eller utelandsk identitetsnummer ved start av abonnementet, vil den respektive dataen bli lagt på feeden. Eventuelle split og merge i Folkeregisteret på brukere som det er satt opp abonnement på må håndteres av Skatteetaten ved at man avslutter gjeldende abonnement og oppretter nytt abonnement.
                 *
                 * Responses:
                 *  - 201 Abonnementet er opprettet.
                 *  - 200 Abonnementet på gitt identitetsnummer eksisterer allerede.
                 *  - 400 Feil i forespørsel.
                 *  - 401 Manglende eller ugyldig Maskinporten-token.
                 *  - 500 Intern feil, abonnement ble ikke opprettet.
                 */
                post("/start") {
                    startAbonnement(abonnementService)
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
                    stoppAbonnement(abonnementService)
                }.describe { stoppAbonnementExamples() }
            }

            /**
             * Hent neste persondata
             *
             * Description: Hent neste persondata fra feeden. Vi skiller mellom flere typer hendelser. OPPDATERT_ADRESSE betyr at det har skjedd en endring på en persons adresse. SLETTET_ADRESSE betyr at en persons adresse er slettet. Dette skjer i utgangspunktet ved adressebeskyttelse. Om man leser en event med denne hendelsestypen så forventes det at konsumenten sletter postadressen til personen. OPPDATERT_UTENLANDSK_ID beskriver endringer i utenlandske identitetsnumre. Returnerer alltid alle tilgjengelige data, så en hendelse med f.eks. OPPDATERT_ADRESSE vil også inneholde gjeldende utenlandsk id. Merk: Endepunktet er under utvikling og returnerer foreløpig faste eksempeldata med OPPDATERT_UTENLANDSK_ID.
             *
             * Responses:
             *  - 200 Persondata i V2-format. Foreløpig faste eksempeldata.
             *  - 401 Manglende eller ugyldig Maskinporten-token.
             */
            post("/feed") {
                call.receive<FeedRequestJson>()

                call.respond(
                    HttpStatusCode.OK,
                    FeedResponseV2Json(
                        abonnementId = "uuid",
                        identitetsnummer = "12342012345",
                        utenlandskPostadresse = null,
                        utenlandskId = listOf(UtenlandskIdResponseJson("123010190B456", "DEU", "Dolly")),
                        hendelsestype = HendelsestypeV2Json.OPPDATERT_UTENLANDSK_ID,
                    ),
                )
            }.describe {
                feedV2Examples()
            }
        }.describe {
            tag("v2-alpha")
        }
    }
}
