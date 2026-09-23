package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.ExampleObject
import io.ktor.openapi.GenericElement
import io.ktor.openapi.Operation
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.infrastructure.route.json.FeedRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.PostadresseFeedResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementRequestJson
import no.nav.utenlandsadresser.infrastructure.route.json.StartAbonnementResponseJson
import no.nav.utenlandsadresser.infrastructure.route.json.StoppAbonnementJson
import kotlin.reflect.typeOf

internal fun Operation.Builder.startAbonnementExamples() {
    requestBody {
        ContentType.Application.Json {
            schema = buildSchema(typeOf<StartAbonnementRequestJson>())
            example(
                "startAbonnement",
                ExampleObject(
                    summary = "Start abonnement",
                    // language=json
                    value = Json.decodeFromString<GenericElement>("""{"identitetsnummer":"12345678901"}"""),
                ),
            )
        }
    }
    responses {
        HttpStatusCode.InternalServerError {
            ContentType.Text.Plain {}
        }
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.Created)) {
            status {
                ContentType.Application.Json {
                    schema = buildSchema(typeOf<StartAbonnementResponseJson>())
                    example(
                        "abonnement",
                        ExampleObject(
                            summary = "Abonnement",
                            // language=json
                            value =
                                Json.decodeFromString<GenericElement>(
                                    """{"abonnementId":"f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f"}""",
                                ),
                        ),
                    )
                }
            }
        }
    }
}

internal fun Operation.Builder.stoppAbonnementExamples() {
    requestBody {
        ContentType.Application.Json {
            schema = buildSchema(typeOf<StoppAbonnementJson>())
            example(
                "stoppAbonnement",
                ExampleObject(
                    summary = "Stopp abonnement",
                    // language=json
                    value =
                        Json.decodeFromString<GenericElement>(
                            """{"abonnementId":"f47b4b9d-3f6d-4f3e-8f2d-3f4b4f3e2d1f"}""",
                        ),
                ),
            )
        }
    }
}

internal fun Operation.Builder.feedRequestExample(
    name: String,
    summary: String,
) {
    requestBody {
        ContentType.Application.Json {
            schema = buildSchema(typeOf<FeedRequestJson>())
            example(
                name,
                ExampleObject(
                    summary = summary,
                    // language=json
                    value = Json.decodeFromString<GenericElement>("""{"løpenummer":"1"}"""),
                ),
            )
        }
    }
}

internal fun Operation.Builder.feedExamples() {
    feedRequestExample("hentPostadresse", "Hent neste postadresse")
    responses {
        HttpStatusCode.OK {
            ContentType.Application.Json {
                schema = buildSchema(typeOf<PostadresseFeedResponseJson>())
                example(
                    "oppdatertAdresse",
                    ExampleObject(
                        summary = "Oppdatert adresse",
                        value =
                            Json.decodeFromString<GenericElement>(
                                """{
                                "abonnementId":"123e4567-e89b-12d3-a456-426614174000",
                                "identitetsnummer":"12345678901",
                                "hendelsestype":"OPPDATERT_ADRESSE",
                                "utenlandskPostadresse":{
                                    "adresselinje1":"Adresselinje 1",
                                    "adresselinje2":"Adresselinje 2",
                                    "adresselinje3":"Adresselinje 3",
                                    "postnummer":"1234",
                                    "poststed":"Poststed",
                                    "landkode":"SE",
                                    "land":"Sverige"
                                }
                            }""",
                            ),
                    ),
                )
                example(
                    "ingenUtenlandskAdresse",
                    ExampleObject(
                        summary = "Ingen utenlandsk adresse",
                        value =
                            Json.decodeFromString<GenericElement>(
                                """{
                                "abonnementId":"123e4567-e89b-12d3-a456-426614174000",
                                "identitetsnummer":"12345678901",
                                "hendelsestype":"OPPDATERT_ADRESSE",
                                "utenlandskPostadresse":null
                            }""",
                            ),
                    ),
                )
                example(
                    "slettetAdresse",
                    ExampleObject(
                        summary = "Slettet adresse",
                        value =
                            Json.decodeFromString<GenericElement>(
                                """{
                                "abonnementId":"123e4567-e89b-12d3-a456-426614174000",
                                "identitetsnummer":"12345678901",
                                "hendelsestype":"SLETTET_ADRESSE",
                                "utenlandskPostadresse":null
                            }""",
                            ),
                    ),
                )
            }
        }
    }
}
