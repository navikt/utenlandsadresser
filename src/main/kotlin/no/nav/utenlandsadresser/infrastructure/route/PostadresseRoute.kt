package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.utenlandsadresser.app.*
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Løpenummer
import no.nav.utenlandsadresser.domain.Organisasjonsnummer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.route.json.*
import no.nav.utenlandsadresser.plugin.OrganisasjonsnummerKey
import no.nav.utenlandsadresser.plugin.VerifyScopeFromJwt
import java.util.*

fun Route.configurePostadresseRoutes(
    scope: Scope,
    abonnementService: AbonnementService,
    feedService: FeedService,
) {
    route("/api/v1/postadresse") {
        install(VerifyScopeFromJwt) {
            this.scope = scope
        }
        route("/abonnement") {
            post<StartAbonnementRequestJson>("/start", {
                summary = "Start abonnement"
                description = """
                                Start abonnement for en person med et gitt identitetsnummer.
                                Om personen har en utenlandsk adresse ved start av abonnementet, vil denne adressen bli lagt på feeden.
                                
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
            }) { json ->
                val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                val identitetsnummer = Identitetsnummer(json.identitetsnummer)

                val abonnement = abonnementService.startAbonnement(identitetsnummer, organisasjonsnummer).getOrElse {
                    when (it) {
                        is StartAbonnementError.AbonnementAlreadyExists -> return@post call.respond(
                            HttpStatusCode.OK,
                            StartAbonnementResponseJson.fromDomain(it.abonnement)
                        )

                        StartAbonnementError.FailedToGetPostadresse -> return@post call.respond(
                            HttpStatusCode.InternalServerError,
                            "Greide ikke å hente postadresse. Opprettet ikke abonnement."
                        )
                    }
                }

                call.respond(HttpStatusCode.Created, StartAbonnementResponseJson.fromDomain(abonnement))
            }
            post<StoppAbonnementJson>("/stopp", {
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
                    HttpStatusCode.BadRequest to {
                        description = "Identitetsnummer må være på 11 siffer."
                    }
                }
            }) { json ->
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

        post<FeedRequestJson>("/feed", {
            summary = "Hent neste postadresse"
            description = "Hent neste postadresse fra feeden."
            protected = true
            securitySchemeName = "Maskinporten"
            request {
                body<FeedRequestJson>()
            }
            response {
                HttpStatusCode.OK to {
                    description = """
                        Returnerer en utenlandsk postadresse om det finnes en.
                        
                        Vi skiller mellom to typer hendelser:
                        - OPPDATERT_ADRESSE: Det har skjedd en endring på en persons adresse. Responsen vil inneholde nåværende adresse.
                        - SLETTET_ADRESSE: En persons adresse er slettet. Dette skjer i utganspunktet ved adressebeskyttelse. Om man leser en event med denne hendelsestypen så forventes det at konsumenten sletter postadressen til personen.
                    """.trimIndent()
                    body<FeedResponseJson>()
                }
                HttpStatusCode.NoContent to {
                    description = "Ingen feedevent på løpenummeret."
                }
                HttpStatusCode.InternalServerError to {
                    description = "Feil ved henting av postadresse."
                }
            }
        }) { json ->
            val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
            val løpenummer = Løpenummer(json.løpenummer.toInt())

            val (feedEvent, postadresse) = feedService.readNext(løpenummer, organisasjonsnummer).getOrElse {
                return@post when (it) {
                    ReadFeedError.FailedToGetPostadresse -> call.respond(
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

