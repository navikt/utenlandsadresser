package no.nav.utenlandsadresser.setup

import arrow.core.toNonEmptySetOrNull
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import no.nav.utenlandsadresser.Plugins
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.plugin.configureCallLogging
import no.nav.utenlandsadresser.plugin.configureMetrics
import no.nav.utenlandsadresser.plugin.configureSerialization
import no.nav.utenlandsadresser.plugin.configureOpenApi
import no.nav.utenlandsadresser.plugin.maskinporten.configureMaskinportenAuthentication
import no.nav.utenlandsadresser.plugin.maskinporten.validateOrganisasjonsnummer
import java.net.URI

/**
 * Setter opp Ktor-plugins som brukes av applikasjonen.
 */
fun Application.setupApplicationPlugins(config: UtenlandsadresserConfig): Plugins {
    val meterRegistry = configureMetrics()
    configureSerialization()
    configureCallLogging()
    configureMaskinportenAuthentication(
        configurationName = "postadresse-abonnement-maskinporten",
        issuer = Issuer(config.maskinporten.issuer),
        requiredScopes =
            config.maskinporten.scopes
                .split(" ")
                .map(::Scope)
                .toNonEmptySetOrNull()
                ?: throw IllegalArgumentException("Missing required scopes"),
        jwkProvider = JwkProviderBuilder(URI.create(config.maskinporten.jwksUri).toURL()).build(),
        jwtValidationBlock = validateOrganisasjonsnummer(config.maskinporten.consumers),
    )
    configureOpenApi()

    return Plugins(
        meterRegistry = meterRegistry,
    )
}
