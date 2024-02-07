package no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.utenlandsadresser.infrastructure.client.RegisteroppslagClient
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.json.GetPostadresseRequestJson
import no.nav.utenlandsadresser.infrastructure.client.http.registeroppslag.json.PostadresseResponseJson
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Postadresse

class RegisteroppslagHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: Url,
) : RegisteroppslagClient {
    override suspend fun getPostadresse(identitetsnummer: Identitetsnummer): Either<RegisteroppslagClient.Error, Postadresse> {
        val response = kotlin.runCatching {
            httpClient.post("$baseUrl/rest/postadresse") {
                contentType(ContentType.Application.Json)
                setBody(
                    GetPostadresseRequestJson(
                        ident = identitetsnummer.value,
                        tema = "INK",
                    )
                )
            }
        }.getOrElse {
            return RegisteroppslagClient.Error.Ukjent("Failed to get postadresse from regoppslag: ${it.message}").left()
        }

        return either {
            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<PostadresseResponseJson>().adresse.toDomain()
                }

                HttpStatusCode.BadRequest -> raise(RegisteroppslagClient.Error.UgyldigForespørsel)
                HttpStatusCode.NotFound -> raise(RegisteroppslagClient.Error.UkjentAdresse)
                HttpStatusCode.Unauthorized -> raise(RegisteroppslagClient.Error.IngenTilgang)
                else -> raise(RegisteroppslagClient.Error.Ukjent("Ukjent feil: ${response.status} ${response.bodyAsText()}"))
            }
        }
    }
}
