package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.app.ReadFeedError
import no.nav.utenlandsadresser.app.StartAbonnementError
import no.nav.utenlandsadresser.app.StoppAbonnementError
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.route.json.FeedRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.FeedResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StoppAbonnementJson
import no.nav.utenlandsadresser.plugin.maskinporten.OrganisasjonsnummerKey
import kotlin.uuid.Uuid

@OptIn(ExperimentalKtorApi::class)
fun Route.configurePostadresseRoutes(
    abonnementService: AbonnementService,
    feedService: FeedService,
) {
    authenticate("postadresse-abonnement-maskinporten") {
        route("/api/v1/postadresse") {
            route("/abonnement") {
                /**
                 * Start abonnement
                 *
                 * Description: Start abonnement for en person med et gitt identitetsnummer. Om personen har en utenlandsk adresse ved start av abonnementet, vil denne adressen bli lagt på feeden. Eventuelle split og merge i Folkeregisteret på brukere som det er satt opp abonnement på må håndteres av Skatteetaten ved at man avslutter gjeldende abonnement og oppretter nytt abonnement.
                 *
                 * Responses:
                 *  - 201 Abonnementet er opprettet.
                 *  - 200 Abonnementet på gitt identitetsnummer eksisterer allerede.
                 *  - 400 Feil i forespørsel.
                 *  - 401 Manglende eller ugyldig Maskinporten-token.
                 *  - 500 Intern feil, abonnement ble ikke opprettet.
                 */
                post("/start") {
                    val json = call.receive<StartAbonnementRequestJson>()
                    val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                    val identitetsnummer = Identitetsnummer(json.identitetsnummer)

                    val abonnement =
                        abonnementService.startAbonnement(identitetsnummer, organisasjonsnummer).getOrElse {
                            when (it) {
                                is StartAbonnementError.AbonnementAlreadyExists -> return@post call.respond(
                                    HttpStatusCode.OK,
                                    StartAbonnementResponseJson.fromDomain(it.abonnement),
                                )

                                StartAbonnementError.FailedToGetPostadresse -> return@post call.respondText(
                                    text = "Greide ikke å hente postadresse. Opprettet ikke abonnement.",
                                    status = HttpStatusCode.InternalServerError,
                                )
                            }
                        }

                    call.respond(HttpStatusCode.Created, StartAbonnementResponseJson.fromDomain(abonnement))
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
                    val json = call.receive<StoppAbonnementJson>()
                    val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                    val abonnementId = Uuid.parse(json.abonnementId)

                    abonnementService.stopAbonnement(abonnementId, organisasjonsnummer).getOrElse {
                        when (it) {
                            StoppAbonnementError.AbonnementNotFound -> call.respond(HttpStatusCode.OK)
                        }
                    }

                    call.respond(HttpStatusCode.OK)
                }.describe {
                    stoppAbonnementExamples()
                }
            }

            /**
             * Hent neste postadresse
             *
             * Description: Hent neste postadresse fra feeden. Returnerer en utenlandsk postadresse om det finnes en. Vi skiller mellom to typer hendelser. OPPDATERT_ADRESSE betyr at det har skjedd en endring på en persons adresse. Responsen vil inneholde nåværende adresse. SLETTET_ADRESSE betyr at en persons adresse er slettet. Dette skjer i utgangspunktet ved adressebeskyttelse. Om man leser en event med denne hendelsestypen så forventes det at konsumenten sletter postadressen til personen.
             *
             * Responses:
             *  - 204 Ingen feed event på gitt løpenummer.
             *  - 401 Manglende eller ugyldig Maskinporten-token.
             *  - 500 Intern feil ved henting av postadresse.
             */
            post("/feed") {
                val json = call.receive<FeedRequestJson>()
                val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                val løpenummer = Løpenummer(json.løpenummer.toInt())

                val (feedEvent, postadresse) =
                    feedService.readNext(løpenummer, organisasjonsnummer).getOrElse {
                        return@post when (it) {
                            ReadFeedError.FailedToGetPostadresse -> {
                                call.respondText(
                                    text = "Greide ikke å hente postadresse",
                                    status = HttpStatusCode.InternalServerError,
                                )
                            }

                            ReadFeedError.FeedEventNotFound -> {
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }

                call.respond(HttpStatusCode.OK, FeedResponseJson.fromDomain(feedEvent, postadresse))
            }.describe {
                feedExamples()
            }
        }
    }
}
