package no.nav.utenlandsadresser.adapter.outbound.registeroppslag

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import no.nav.utenlandsadresser.adapter.outbound.registeroppslag.json.GetPostadresseRequestJson
import no.nav.utenlandsadresser.adapter.outbound.registeroppslag.json.PostadresseResponseJson
import no.nav.utenlandsadresser.adapter.outbound.registeroppslag.json.RegisteroppslagAdressebeskyttelse
import no.nav.utenlandsadresser.application.port.outbound.GetPostadresseError
import no.nav.utenlandsadresser.application.port.outbound.RegisteroppslagClient
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Postadresse

class RegisteroppslagHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: Url,
    private val behandlingsnummer: BehandlingskatalogBehandlingsnummer,
) : RegisteroppslagClient {
    override suspend fun getPostadresse(identitetsnummer: Identitetsnummer): Either<GetPostadresseError, Postadresse> {
        val response =
            kotlin
                .runCatching {
                    httpClient.post("$baseUrl/rest/postadresse") {
                        header("Behandlingsnummer", behandlingsnummer.value)
                        contentType(ContentType.Application.Json)
                        setBody(
                            GetPostadresseRequestJson(
                                ident = identitetsnummer.value,
                                filtrerAdressebeskyttelse =
                                    setOf(
                                        RegisteroppslagAdressebeskyttelse.STRENGT_FORTROLIG_UTLAND,
                                        RegisteroppslagAdressebeskyttelse.STRENGT_FORTROLIG,
                                        RegisteroppslagAdressebeskyttelse.FORTROLIG,
                                    ),
                            ),
                        )
                    }
                }.getOrElse {
                    return GetPostadresseError.UkjentFeil("Failed to get postadresse from regoppslag: ${it.message}").left()
                }

        return either {
            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<PostadresseResponseJson>().adresse.toDomain()
                }

                HttpStatusCode.BadRequest -> {
                    raise(GetPostadresseError.UgyldigForespørsel)
                }

                HttpStatusCode.NoContent,
                HttpStatusCode.NotFound,
                HttpStatusCode.Gone,
                -> {
                    raise(GetPostadresseError.UkjentAdresse)
                }

                HttpStatusCode.Unauthorized -> {
                    raise(GetPostadresseError.IngenTilgang)
                }

                HttpStatusCode.Conflict if (response.headers.contains("Nav-Reason-Code", "falsk_identitet")) -> {
                    raise(GetPostadresseError.FalskIdentiet)
                }

                else -> {
                    raise(GetPostadresseError.UkjentFeil("Ukjent feil: ${response.status} ${response.bodyAsText()}"))
                }
            }
        }
    }
}
