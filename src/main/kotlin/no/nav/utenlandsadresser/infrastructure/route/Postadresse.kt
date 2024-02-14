package no.nav.utenlandsadresser.infrastructure.route

import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.utenlandsadresser.app.*
import no.nav.utenlandsadresser.domain.*
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
            post<StartAbonnementJson>("/start") { json ->
                val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
                val identitetsnummer = Identitetsnummer(json.identitetsnummer)
                    .getOrElse {
                        return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig identitetsnummer")
                    }

                abonnementService.startAbonnement(identitetsnummer, organisasjonsnummer).getOrElse {
                    when (it) {
                        StartAbonnementError.AbonnementAlreadyExists -> call.respond(HttpStatusCode.NoContent)
                        StartAbonnementError.FailedToGetPostadresse -> call.respond(
                            HttpStatusCode.InternalServerError,
                            "Greide ikke å hente postadresse. Ingen abonnement opprettet.d",
                        )
                    }
                }

                call.respond(HttpStatusCode.Created)
            }

            post<StoppAbonnementJson>("/stopp") { json ->
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
        post<FeedRequestJson>("/feed") { json ->
            val organisasjonsnummer = Organisasjonsnummer(call.attributes[OrganisasjonsnummerKey])
            val løpenummer = Løpenummer(json.løpenummer.toInt())

            val postadresse = feedService.readFeed(løpenummer, organisasjonsnummer).getOrElse {
                return@post when (it) {
                    ReadFeedError.FailedToGetPostadresse -> call.respond(
                        HttpStatusCode.InternalServerError,
                        "Greide ikke å hente postadresse",
                    )

                    ReadFeedError.FeedEventNotFound -> call.respond(HttpStatusCode.NoContent)
                    ReadFeedError.PostadresseNotFound -> call.respond(FeedResponseJson.empty())
                }
            }

            call.respond(FeedResponseJson.fromDomain(postadresse))
        }
    }
}

@Serializable
data class FeedResponseJson(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String?,
    val land: String?,
) {
    companion object {
        fun fromDomain(postadresse: Postadresse.Utenlandsk): FeedResponseJson = FeedResponseJson(
            adresselinje1 = postadresse.adresselinje1?.value,
            adresselinje2 = postadresse.adresselinje2?.value,
            adresselinje3 = postadresse.adresselinje3?.value,
            postnummer = postadresse.postnummer?.value,
            poststed = postadresse.poststed?.value,
            landkode = postadresse.landkode.value,
            land = postadresse.land.value,
        )

        fun empty(): FeedResponseJson = FeedResponseJson(
            adresselinje1 = null,
            adresselinje2 = null,
            adresselinje3 = null,
            postnummer = null,
            poststed = null,
            landkode = null,
            land = null,
        )
    }
}

@Serializable
data class FeedRequestJson(
    val løpenummer: String,
)

@Serializable
data class StartAbonnementJson(
    val identitetsnummer: String,
)

@Serializable
data class StoppAbonnementJson(
    val identitetsnummer: String,
)
