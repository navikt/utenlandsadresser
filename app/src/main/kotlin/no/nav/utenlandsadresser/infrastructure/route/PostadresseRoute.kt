package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
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
import no.nav.utenlandsadresser.infrastructure.route.json.FeedRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.FeedResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.HendelsestypeJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StoppAbonnementJson
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskPostadresseJson
import no.nav.utenlandsadresser.plugin.maskinporten.OrganisasjonsnummerKey
import java.util.*

fun Route.configurePostadresseRoutes(
    abonnementService: AbonnementService,
    feedService: FeedService,
) {
    authenticate("postadresse-abonnement-maskinporten") {
        route("/api/v1/postadresse") {
            route("/abonnement") {
                post<StartAbonnementRequestJson>("/start", RouteConfig::documentStartRoute) { json ->
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
                post<StoppAbonnementJson>("/stopp", RouteConfig::documentStoppRoute) { json ->
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

            post<FeedRequestJson>("/feed", RouteConfig::documentFeedRoute) { json ->
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

private fun RouteConfig.documentStartRoute() {
    summary = "Start abonnement"
    description =
        """
        Start abonnement for en person med et gitt identitetsnummer.
        Om personen har en utenlandsk adresse ved start av abonnementet, vil denne adressen bli lagt på feeden.
        
        Eventuelle split og merge i Folkeregisterer på brukere som det er satt opp abonnement på må håndteres av Skatteetaten ved at man avslutter gjeldende abonnement og oppretter nytt abonnement.
        
        Returnerer en UUID som referanse til abonnementet.
        """.trimIndent()
    protected = true
    securitySchemeNames("Maskinporten")
    request {
        body<StartAbonnementRequestJson> {
            example("Start abonnement request") {
                value =
                    StartAbonnementRequestJson(
                        identitetsnummer = "12345678901",
                    )
            }
        }
    }
    response {
        HttpStatusCode.Created to {
            description = "Abonnementet ble opprettet. Returnerer referanse til abonnementet."
            body<StartAbonnementResponseJson> {
                example("Start abonnement response") {
                    value =
                        StartAbonnementResponseJson(
                            abonnementId = "f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f",
                        )
                }
            }
        }
        HttpStatusCode.OK to {
            description =
                "Abonnement eksisterer fra før. Ingen abonnement blir opprettet. Returnerer referanse til eksisterende abonnement."
            body<StartAbonnementResponseJson> {
                example("Start abonnement response") {
                    value =
                        StartAbonnementResponseJson(
                            abonnementId = "f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f",
                        )
                }
            }
        }
        HttpStatusCode.BadRequest to {
            description = "Identitetsnummer må være på 11 siffer."
        }
        HttpStatusCode.InternalServerError to {
            description = "Fikk feil ved henting av postadresse. Ingen abonnement blir opprettet."
        }
    }
}

private fun RouteConfig.documentStoppRoute() {
    summary = "Stopp abonnement"
    description = "Stopp abonnement med en gitt referanse."
    protected = true
    securitySchemeNames("Maskinporten")
    request {
        body<StoppAbonnementJson> {
            example("Stopp abonnement request") {
                value =
                    StoppAbonnementJson(
                        abonnementId = "f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f",
                    )
            }
        }
    }
    response {
        HttpStatusCode.OK to {
            description = "Abonnementet ble stoppet eller var allerede stoppet."
        }
    }
}

private fun RouteConfig.documentFeedRoute() {
    summary = "Hent neste postadresse"
    description = "Hent neste postadresse fra feeden."
    protected = true
    securitySchemeNames("Maskinporten")
    request {
        body<FeedRequestJson> {
            example("Hent neste postadresse request") {
                value =
                    FeedRequestJson(
                        løpenummer = "1",
                    )
            }
        }
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
                example("Respons med adresse") {
                    value =
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
                        )
                }
                example("Respons med manglende/slettet adresse") {
                    value =
                        FeedResponseJson(
                            abonnementId = abonnementId,
                            identitetsnummer = identitetsnummer,
                            hendelsestype = HendelsestypeJson.OPPDATERT_ADRESSE,
                            utenlandskPostadresse = null,
                        )
                }
                example("Respons med hendelsestype SLETTET_ADRESSE") {
                    value =
                        FeedResponseJson(
                            abonnementId = abonnementId,
                            identitetsnummer = identitetsnummer,
                            hendelsestype = HendelsestypeJson.SLETTET_ADRESSE,
                            utenlandskPostadresse = null,
                        )
                }
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
