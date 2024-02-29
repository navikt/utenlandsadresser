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

fun Route.configurePostadresseRoutes(
    scope: Scope,
    abonnementService: AbonnementService,
    feedService: FeedService,
) {
    route("/postadresse") {
        install(VerifyScopeFromJwt) {
            this.scope = scope
        }
        route("/abonnement") {
            post<StartAbonnementRequestJson>("/start/v2", {
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
            }) {
                call.respond(HttpStatusCode.NotImplemented)
            }
            post<StartAbonnementRequestJson>("/start", {
                summary = "Start abonnement"
                description = """
                                Start abonnement for en person med et gitt identitetsnummer.
                                Om personen har en utenlandsk adresse ved start av abonnementet, vil denne adressen bli lagt på feeden.
                            """.trimIndent()
                protected = true
                securitySchemeName = "Maskinporten"
                request {
                    body<StartAbonnementRequestJson>()
                }
                response {
                    HttpStatusCode.Created to {
                        description = "Abonnementet ble opprettet."
                    }
                    HttpStatusCode.OK to {
                        description = "Abonnement eksisterer fra før. Ingen abonnement blir opprettet."
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
                    .getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig identitetsnummer.")
                    }

                abonnementService.startAbonnement(identitetsnummer, organisasjonsnummer).getOrElse {
                    when (it) {
                        StartAbonnementError.AbonnementAlreadyExists -> call.respond(
                            HttpStatusCode.OK,
                            "Abonnement eksisterer fra før. Oppretter ikke nytt abonnement.",
                        )

                        StartAbonnementError.FailedToGetPostadresse -> call.respond(
                            HttpStatusCode.InternalServerError,
                            "Greide ikke å hente postadresse. Opprettet ikke abonnement.",
                        )
                    }
                }

                call.respond(HttpStatusCode.Created)
            }

            post<StoppAbonnementJsonV2>("/stopp/v2", {
                summary = "Stopp abonnement"
                description = "Stopp abonnement med en gitt referanse."
                protected = true
                securitySchemeName = "Maskinporten"
                request {
                    body<StoppAbonnementJsonV2>()
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Abonnementet ble stoppet eller var allerede stoppet."
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Identitetsnummer må være på 11 siffer."
                    }
                }
            }) {
                call.respond(HttpStatusCode.NotImplemented)
            }

            post<StoppAbonnementJson>("/stopp", {
                summary = "Stopp abonnement"
                description = "Stopp abonnement for en person med et gitt identitetsnummer."
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
                val identitetsnummer = Identitetsnummer(json.identitetsnummer)
                    .getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, "Invalid identitetsnummer")
                    }

                abonnementService.stopAbonnement(identitetsnummer, organisasjonsnummer).getOrElse {
                    when (it) {
                        StoppAbonnementError.AbonnementNotFound -> call.respond(HttpStatusCode.OK)
                    }
                }

                call.respond(HttpStatusCode.OK)
            }
        }

        post<FeedRequestJsonV2>("/feed/v2", {
            summary = "Hent neste postadresse"
            description = "Hent neste postadresse fra feeden."
            protected = true
            securitySchemeName = "Maskinporten"
            request {
                body<FeedRequestJsonV2>()
            }
            response {
                HttpStatusCode.OK to {
                    description =
                        "Postadresse hentet. Om alle feltene er `null` betyr det at det ikke finnes en utenlandsadresse."
                    body<FeedResponseJsonV2>()
                }
                HttpStatusCode.NoContent to {
                    description = "Ingen feedevent på løpenummeret."
                }
                HttpStatusCode.InternalServerError to {
                    description = "Feil ved henting av postadresse."
                }
            }

        }) {

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
                    description =
                        "Postadresse hentet. Om alle feltene er `null` betyr det at det ikke finnes en utenlandsadresse."
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

            val (identitetsnummer, postadresse) = feedService.readNext(løpenummer, organisasjonsnummer).getOrElse {
                return@post when (it) {
                    ReadFeedError.FailedToGetPostadresse -> call.respond(
                        HttpStatusCode.InternalServerError,
                        "Greide ikke å hente postadresse",
                    )

                    ReadFeedError.FeedEventNotFound -> call.respond(HttpStatusCode.NoContent)
                }
            }

            call.respond(FeedResponseJson.fromDomain(identitetsnummer, postadresse))
        }
    }
}

