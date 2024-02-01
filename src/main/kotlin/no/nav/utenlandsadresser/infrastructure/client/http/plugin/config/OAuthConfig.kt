package no.nav.utenlandsadresser.infrastructure.client.http.plugin.config

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import io.ktor.http.*
import no.nav.utenlandsadresser.domain.ClientId
import no.nav.utenlandsadresser.domain.ClientSecret
import no.nav.utenlandsadresser.domain.GrantType
import no.nav.utenlandsadresser.domain.Scope

data class OAuthConfig private constructor(
    val tokenEndpoint: Url,
    val clientId: ClientId,
    val clientSecret: ClientSecret,
    val scope: Scope,
    val grantType: GrantType,
) {
    companion object {
        operator fun invoke(
            tokenEndpoint: String?,
            clientId: String?,
            clientSecret: String?,
            scope: String?,
            grantType: String?,
        ): EitherNel<Error, OAuthConfig> = either {
            zipOrAccumulate(
                {
                    ensureNotNull(tokenEndpoint) { Error.TokenEndpointMissing }
                    runCatching {
                        Url(tokenEndpoint)
                    }.getOrElse {
                        raise(Error.TokenEndpointInvalid(tokenEndpoint))
                    }
                },
                { ensureNotNull(clientId) { Error.ClientIdMissing } },
                { ensureNotNull(clientSecret) { Error.ClientSecretMissing } },
                { ensureNotNull(scope) { Error.ScopeMissing } },
                { ensureNotNull(grantType) { Error.GrantTypeMissing } }
            ) { tokenEndpoint, clientId, clientSecret, scope, grantType ->
                OAuthConfig(
                    tokenEndpoint = tokenEndpoint,
                    clientId = ClientId(clientId),
                    clientSecret = ClientSecret(clientSecret),
                    scope = Scope(scope),
                    grantType = GrantType(grantType),
                )
            }
        }
    }

    sealed class Error {
        data object TokenEndpointMissing : Error()
        data class TokenEndpointInvalid(
            val tokenEndpoint: String,
        ) : Error()

        data object ClientIdMissing : Error()
        data object ClientSecretMissing : Error()
        data object ScopeMissing : Error()
        data object GrantTypeMissing : Error()
    }
}