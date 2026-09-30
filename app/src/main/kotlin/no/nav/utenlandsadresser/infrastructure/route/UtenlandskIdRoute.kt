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
import no.nav.utenlandsadresser.app.ReadUtenlandskIdFeedError
import no.nav.utenlandsadresser.app.StartUtenlandskIdAbonnementError
import no.nav.utenlandsadresser.app.StoppAbonnementError
import no.nav.utenlandsadresser.app.UtenlandskIdAbonnementService
import no.nav.utenlandsadresser.app.UtenlandskIdFeedService
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.infrastructure.route.json.FeedRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StoppAbonnementJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdFeedResponseJson
import no.nav.utenlandsadresser.plugin.maskinporten.OrganisasjonsnummerKey
import kotlin.uuid.Uuid

const val UTENLANDSK_ID_MASKINPORTEN_AUTH = "utenlandskid-abonnement-maskinporten"

/**
 * Abonnement på utenlandsk id.
 *
 * Dette er et eget abonnement, adskilt fra postadresse-abonnementet, med egne tabeller og eget Maskinporten-scope.
 * Feeden lagrer bare identitetsnummer og hendelsestype. Utenlandske id-er hentes på nytt når feeden leses.
 */
@OptIn(ExperimentalKtorApi::class)
fun Route.configureUtenlandskIdRoutes(
    abonnementService: UtenlandskIdAbonnementService,
    feedService: UtenlandskIdFeedService,
) {
    authenticate(UTENLANDSK_ID_MASKINPORTEN_AUTH) {
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
                    val json = call.receive<StartAbonnementRequestJson>()
                    val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])

                    val abonnement =
                        abonnementService
                            .startAbonnement(Identitetsnummer(json.identitetsnummer), organisasjonsnummer)
                            .getOrElse {
                                return@post when (it) {
                                    is StartUtenlandskIdAbonnementError.AbonnementAlreadyExists -> {
                                        call.respond(HttpStatusCode.OK, StartAbonnementResponseJson.fromDomain(it.abonnement))
                                    }

                                    StartUtenlandskIdAbonnementError.FailedToGetUtenlandskId -> {
                                        call.respondText(
                                            text = "Greide ikke å hente utenlandsk id. Opprettet ikke abonnement.",
                                            status = HttpStatusCode.InternalServerError,
                                        )
                                    }
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

                    abonnementService.stopAbonnement(Uuid.parse(json.abonnementId), organisasjonsnummer).getOrElse {
                        when (it) {
                            StoppAbonnementError.AbonnementNotFound -> Unit
                        }
                    }

                    call.respond(HttpStatusCode.OK)
                }.describe {
                    stoppAbonnementExamples()
                }
            }

            /**
             * Hent neste utenlandsk id
             *
             * Description: Hent neste hendelse fra feeden. OPPDATERT_UTENLANDSK_ID betyr at personen har eller har hatt utenlandsk id. Responsen inneholder personens gjeldende utenlandske id-er. Listen kan være tom om id-en er opphørt etter at hendelsen ble lagt på feeden.
             *
             * Responses:
             *  - 204 Ingen feed event på gitt løpenummer.
             *  - 401 Manglende eller ugyldig Maskinporten-token.
             *  - 500 Intern feil ved henting av utenlandsk id.
             */
            post("/feed") {
                val json = call.receive<FeedRequestJson>()
                val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                val løpenummer = Løpenummer(json.løpenummer.toInt())

                val (feedEvent, utenlandskeIdentiteter) =
                    feedService.readNext(løpenummer, organisasjonsnummer).getOrElse {
                        return@post when (it) {
                            ReadUtenlandskIdFeedError.FailedToGetUtenlandskId -> {
                                call.respondText(
                                    text = "Greide ikke å hente utenlandsk id",
                                    status = HttpStatusCode.InternalServerError,
                                )
                            }

                            ReadUtenlandskIdFeedError.FeedEventNotFound -> {
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }

                call.respond(
                    HttpStatusCode.OK,
                    UtenlandskIdFeedResponseJson.fromDomain(feedEvent, utenlandskeIdentiteter),
                )
            }.describe {
                utenlandskIdFeedExamples()
            }
        }.describe {
            tag("v1")
        }
    }
}
