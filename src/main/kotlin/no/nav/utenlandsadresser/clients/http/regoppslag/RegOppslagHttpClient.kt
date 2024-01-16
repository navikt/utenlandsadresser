package no.nav.utenlandsadresser.clients.http.regoppslag

import arrow.core.Either
import arrow.core.raise.either
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.utenlandsadresser.clients.RegOppslagClient
import no.nav.utenlandsadresser.clients.http.regoppslag.json.GetPostadresseRequestJson
import no.nav.utenlandsadresser.clients.http.regoppslag.json.PostadresseResponseJson
import no.nav.utenlandsadresser.domain.*

class RegOppslagHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: Url,
) : RegOppslagClient {
    override suspend fun getPostadresse(fødselsnummer: Fødselsnummer): Either<RegOppslagClient.Error, Postadresse> {
        val response = httpClient.post("$baseUrl/rest/postadresse") {
            contentType(ContentType.Application.Json)
            setBody(
                GetPostadresseRequestJson(
                    ident = fødselsnummer.value,
                    tema = "INK",
                )
            )
        }

        return either {
            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<PostadresseResponseJson>().adresse.toDomain()
                }

                HttpStatusCode.BadRequest -> raise(RegOppslagClient.Error.UgyldigForespørsel)
                HttpStatusCode.NotFound -> raise(RegOppslagClient.Error.UkjentAdresse)
                HttpStatusCode.Unauthorized -> raise(RegOppslagClient.Error.IngenTilgang)
                else -> return Either.Left(RegOppslagClient.Error.Teknisk)
            }
        }
    }
}
