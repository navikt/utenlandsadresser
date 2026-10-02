package no.nav.utenlandsadresser.infrastructure.client.http.pdl

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.request.header
import no.nav.utenlandsadresser.domain.BehandlingskatalogBehandlingsnummer
import no.nav.utenlandsadresser.domain.Identitetsnummer
import no.nav.utenlandsadresser.domain.Iso3166Alpha3
import no.nav.utenlandsadresser.domain.UtenlandskIdentitet
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetKilde
import no.nav.utenlandsadresser.domain.UtenlandskIdentitetsnummer
import no.nav.utenlandsadresser.infrastructure.client.HentUtenlandskId
import no.nav.utenlandsadresser.infrastructure.client.pdl.generated.HentPerson
import no.nav.utenlandsadresser.infrastructure.client.pdl.generated.enums.Endringstype
import org.slf4j.LoggerFactory
import java.util.UUID
import no.nav.utenlandsadresser.infrastructure.client.pdl.generated.hentperson.UtenlandskIdentifikasjonsnummer as PdlUtenlandskIdentifikasjonsnummer

class PdlGraphQLClient(
    private val graphQLClient: GraphQLKtorClient,
    private val behandlingsnummer: BehandlingskatalogBehandlingsnummer?,
) : HentUtenlandskId {
    private val logger = LoggerFactory.getLogger(PdlGraphQLClient::class.java)

    override suspend fun hentUtenlandskIdentitet(
        identitetsnummer: Identitetsnummer,
    ): Either<HentUtenlandskId.Error, List<UtenlandskIdentitet>> =
        either {
            val callId = UUID.randomUUID().toString()
            val response =
                Either
                    .catch {
                        graphQLClient.execute(HentPerson(HentPerson.Variables(identitetsnummer.value))) {
                            behandlingsnummer?.let { header("Behandlingsnummer", it.value) }
                            header("Nav-Call-Id", callId)
                        }
                    }.getOrElse {
                        logger.error("Feil ved kall til PDL. Nav-Call-Id: {}", callId, it)
                        raise(HentUtenlandskId.Error.Kommunikasjonsfeil)
                    }

            val errors = response.errors
            if (!errors.isNullOrEmpty()) {
                errors.forEach { error ->
                    logger.error(
                        "PDL returnerte GraphQL-feil: {} (sti: {}). Nav-Call-Id: {}",
                        error.message,
                        error.path,
                        callId,
                    )
                }
                raise(HentUtenlandskId.Error.FeilIRespons)
            }

            response.data
                ?.hentPerson
                ?.utenlandskIdentifikasjonsnummer
                .orEmpty()
                .filter { it.metadata.master == "PDL" }
                .filterNot { it.opphoert }
                .map { pdlIdentitet ->
                    pdlIdentitet.toDomain().getOrElse {
                        logger.error("Feil ved mapping av utenlandsk identitet: {}. Nav-Call-Id: {}", it, callId)
                        raise(it)
                    }
                }
        }

    private fun PdlUtenlandskIdentifikasjonsnummer.toDomain(): Either<HentUtenlandskId.Error, UtenlandskIdentitet> =
        either {
            val landkode =
                Iso3166Alpha3.from(utstederland)
                    ?: raise(HentUtenlandskId.Error.UgyldigUtstederland)
            val kilde =
                metadata.endringer
                    .asSequence()
                    .filter { it.type == Endringstype.OPPRETT || it.type == Endringstype.KORRIGER }
                    .maxByOrNull { it.registrert }
                    ?.kilde
                    ?: raise(HentUtenlandskId.Error.ManglerKildeForUtenlandskIdentitet)

            UtenlandskIdentitet(
                identitetsnummer = UtenlandskIdentitetsnummer(identifikasjonsnummer),
                utstederland = landkode,
                kilde = UtenlandskIdentitetKilde(kilde),
            )
        }
}
