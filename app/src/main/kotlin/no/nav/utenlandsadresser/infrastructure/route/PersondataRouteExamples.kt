package no.nav.utenlandsadresser.infrastructure.route

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.ExampleObject
import io.ktor.openapi.GenericElement
import io.ktor.openapi.Operation
import kotlinx.serialization.json.Json
import no.nav.utenlandsadresser.infrastructure.route.json.FeedResponseV2Json
import kotlin.reflect.typeOf

internal fun Operation.Builder.feedV2Examples() {
    feedRequestExample("hentPersondata", "Hent neste persondata")
    responses {
        HttpStatusCode.OK {
            ContentType.Application.Json {
                schema = buildSchema(typeOf<FeedResponseV2Json>())
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
                                "utenlandskPostadresse":null,
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
                    "oppdatertAdresse",
                    ExampleObject(
                        summary = "Oppdatert adresse (V2-format)",
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
                                },
                                "utenlandskId":[]
                            }""",
                            ),
                    ),
                )
                example(
                    "ingenUtenlandskAdresse",
                    ExampleObject(
                        summary = "Ingen utenlandsk adresse (V2-format)",
                        value =
                            Json.decodeFromString<GenericElement>(
                                """{
                                "abonnementId":"123e4567-e89b-12d3-a456-426614174000",
                                "identitetsnummer":"12345678901",
                                "hendelsestype":"OPPDATERT_ADRESSE",
                                "utenlandskPostadresse":null,
                                "utenlandskId":[]
                            }""",
                            ),
                    ),
                )
                example(
                    "slettetAdresse",
                    ExampleObject(
                        summary = "Slettet adresse (V2-format)",
                        value =
                            Json.decodeFromString<GenericElement>(
                                """{
                                "abonnementId":"123e4567-e89b-12d3-a456-426614174000",
                                "identitetsnummer":"12345678901",
                                "hendelsestype":"SLETTET_ADRESSE",
                                "utenlandskPostadresse":null,
                                "utenlandskId":[]
                            }""",
                            ),
                    ),
                )
            }
        }
    }
}
