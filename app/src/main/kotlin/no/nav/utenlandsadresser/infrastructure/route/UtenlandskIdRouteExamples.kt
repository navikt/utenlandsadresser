package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.ExampleObject
import io.ktor.openapi.GenericElement
import io.ktor.openapi.Operation
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.infrastructure.route.json.UtenlandskIdFeedResponseJson
import kotlin.reflect.typeOf

internal fun Operation.Builder.utenlandskIdFeedExamples() {
    feedRequestExample("hentUtenlandskId", "Hent neste utenlandsk id")
    responses {
        HttpStatusCode.OK {
            ContentType.Application.Json {
                schema = buildSchema(typeOf<UtenlandskIdFeedResponseJson>())
                example(
                    "oppdatertUtenlandskId",
                    ExampleObject(
                        summary = "Oppdatert utenlandsk ID",
                        value =
                            Json.decodeFromString<GenericElement>(
                                """{
                                "abonnementId":"123e4567-e89b-12d3-a456-426614174000",
                                "identitetsnummer":"12345678901",
                                "hendelsestype":"OPPDATERT_UTENLANDSK_ID",
                                "utenlandskId":[{
                                    "identitetsnummer":"123010190B456",
                                    "utstederland":"DEU",
                                    "kilde":"Dolly"
                                }]
                            }""",
                            ),
                    ),
                )
                example(
                    "ingenUtenlandskId",
                    ExampleObject(
                        summary = "Ingen utenlandsk id",
                        value =
                            Json.decodeFromString<GenericElement>(
                                """{
                                "abonnementId":"123e4567-e89b-12d3-a456-426614174000",
                                "identitetsnummer":"12345678901",
                                "hendelsestype":"OPPDATERT_UTENLANDSK_ID",
                                "utenlandskId":[]
                            }""",
                            ),
                    ),
                )
            }
        }
    }
}
