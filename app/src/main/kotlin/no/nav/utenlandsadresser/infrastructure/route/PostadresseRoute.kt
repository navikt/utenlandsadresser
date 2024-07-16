package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import io.github.smiley4.ktorswaggerui.dsl.OpenApiRoute
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.utenlandsadresser.app.AbonnementService
import no.nav.utenlandsadresser.app.FeedService
import no.nav.utenlandsadresser.app.ReadFeedError
import no.nav.utenlandsadresser.app.StartAbonnementError
import no.nav.utenlandsadresser.app.StoppAbonnementError
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.route.json.FeedRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.FeedResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.HendelsestypeJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StoppAbonnementJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskPostadresseJson
import no.nav.utenlandsadresser.plugin.maskinporten.OrganisasjonsnummerKey
import no.nav.utenlandsadresser.plugin.maskinporten.protectWithOrganisasjonsnummer
import no.nav.utenlandsadresser.plugin.maskinporten.protectWithScopes
import java.util.*

fun Route.configurePostadresseRoutes(
    scope: Scope,
    consumers: Set<Organisasjonsnummer>,
    abonnementService: AbonnementService,
    feedService: FeedService,
) {
    authenticate("maskinporten") {
        route("/api/v1/postadresse") {
            protectWithScopes(setOf(scope))
            protectWithOrganisasjonsnummer(consumers)
            route("/abonnement") {
                post<StartAbonnementRequestJson>("/start", OpenApiRoute::documentStartRoute) { json ->
                    val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                    val identitetsnummer = Identitetsnummer(json.identitetsnummer)

                    val abonnement =
                        abonnementService.startAbonnement(identitetsnummer, organisasjonsnummer).getOrElse {
                            when (it) {
                                is StartAbonnementError.AbonnementAlreadyExists -> return@post call.respond(
                                    HttpStatusCode.OK,
                                    StartAbonnementResponseJson.fromDomain(it.abonnement),
                                )

                                StartAbonnementError.FailedToGetPostadresse -> return@post call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Greide ikke å hente postadresse. Opprettet ikke abonnement.",
                                )
                            }
                        }

                    call.respond(HttpStatusCode.Created, StartAbonnementResponseJson.fromDomain(abonnement))
                }
                post<StoppAbonnementJson>("/stopp", OpenApiRoute::documentStoppRoute) { json ->
                    val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                    val abonnementId = UUID.fromString(json.abonnementId)

                    abonnementService.stopAbonnement(abonnementId, organisasjonsnummer).getOrElse {
                        when (it) {
                            StoppAbonnementError.AbonnementNotFound -> call.respond(HttpStatusCode.OK)
                        }
                    }

                    call.respond(HttpStatusCode.OK)
                }
            }

            post<FeedRequestJson>("/feed", OpenApiRoute::documentFeedRoute) { json ->
                val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                val løpenummer = Løpenummer(json.løpenummer.toInt())

                val (feedEvent, postadresse) =
                    feedService.readNext(løpenummer, organisasjonsnummer).getOrElse {
                        return@post when (it) {
                            ReadFeedError.FailedToGetPostadresse ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Greide ikke å hente postadresse",
                                )

                            ReadFeedError.FeedEventNotFound -> call.respond(HttpStatusCode.NoContent)
                        }
                    }

                call.respond(FeedResponseJson.fromDomain(feedEvent, postadresse))
            }
        }
    }
}

private fun OpenApiRoute.documentStartRoute() {
    summary = "Start abonnement"
    description =
        """
        Start abonnement for en person med et gitt identitetsnummer.
        Om personen har en utenlandsk adresse ved start av abonnementet, vil denne adressen bli lagt på feeden.
        
        Eventuelle split og merge i Folkeregisterer på brukere som det er satt opp abonnement på må håndteres av Skatteetaten ved at man avslutter gjeldende abonnement og oppretter nytt abonnement.
        
        Returnerer en UUID som referanse til abonnementet.
        """.trimIndent()
    protected = true
    securitySchemeName = "Maskinporten"
    request {
        body<StartAbonnementRequestJson>()
    }
    response {
        HttpStatusCode.Created to {
            description = "Abonnementet ble opprettet. Returnerer referanse til abonnementet."
            body<StartAbonnementResponseJson>()
        }
        HttpStatusCode.OK to {
            description =
                "Abonnement eksisterer fra før. Ingen abonnement blir opprettet. Returnerer referanse til eksisterende abonnement."
            body<StartAbonnementResponseJson>()
        }
        HttpStatusCode.BadRequest to {
            description = "Identitetsnummer må være på 11 siffer."
        }
        HttpStatusCode.InternalServerError to {
            description = "Fikk feil ved henting av postadresse. Ingen abonnement blir opprettet."
        }
    }
}

private fun OpenApiRoute.documentStoppRoute() {
    summary = "Stopp abonnement"
    description = "Stopp abonnement med en gitt referanse."
    protected = true
    securitySchemeName = "Maskinporten"
    request {
        body<StoppAbonnementJson>()
    }
    response {
        HttpStatusCode.OK to {
            description = "Abonnementet ble stoppet eller var allerede stoppet."
        }
    }
}

private fun OpenApiRoute.documentFeedRoute() {
    summary = "Hent neste postadresse"
    description = "Hent neste postadresse fra feeden."
    protected = true
    securitySchemeName = "Maskinporten"
    request {
        body<FeedRequestJson>()
    }
    response {
        HttpStatusCode.OK to {
            description =
                """
                Returnerer en utenlandsk postadresse om det finnes en.
                
                Vi skiller mellom to typer hendelser:
                - OPPDATERT_ADRESSE: Det har skjedd en endring på en persons adresse. Responsen vil inneholde nåværende adresse.
                - SLETTET_ADRESSE: En persons adresse er slettet. Dette skjer i utganspunktet ved adressebeskyttelse. Om man leser en event med denne hendelsestypen så forventes det at konsumenten sletter postadressen til personen.
                """.trimIndent()
            body<FeedResponseJson> {
                val abonnementId = "123e4567-e89b-12d3-a456-426614174000"
                val identitetsnummer = "12345678901"
                example(
                    "Respons med adresse",
                    FeedResponseJson(
                        abonnementId = abonnementId,
                        identitetsnummer = identitetsnummer,
                        hendelsestype = HendelsestypeJson.OPPDATERT_ADRESSE,
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
                    ),
                )
                example(
                    "Respons med manglende/slettet adresse",
                    FeedResponseJson(
                        abonnementId = abonnementId,
                        identitetsnummer = identitetsnummer,
                        hendelsestype = HendelsestypeJson.OPPDATERT_ADRESSE,
                        utenlandskPostadresse = null,
                    ),
                )
                example(
                    "Respons med hendelsestype SLETTET_ADRESSE",
                    FeedResponseJson(
                        abonnementId = abonnementId,
                        identitetsnummer = identitetsnummer,
                        hendelsestype = HendelsestypeJson.SLETTET_ADRESSE,
                        utenlandskPostadresse = null,
                    ),
                ) {
                    description =
                        """
                        Brukes for å be konsumenten slette postadressen til personen.
                        I utgangspunktet brukes denne hendelsestypen når en person får adressebeskyttelse.
                        """.trimIndent()
                }
            }
        }
        HttpStatusCode.NoContent to {
            description = "Ingen feedevent på løpenummeret."
        }
        HttpStatusCode.InternalServerError to {
            description = "Feil ved henting av postadresse."
        }
    }
}
