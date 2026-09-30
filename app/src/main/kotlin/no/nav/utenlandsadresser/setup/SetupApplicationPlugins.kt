package no.nav.utenlandsadresser.setup

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import no.nav.utenlandsadresser.Plugins
import no.nav.utenlandsadresser.config.UtenlandsadresserConfig
import no.nav.utenlandsadresser.domain.Issuer
import no.nav.utenlandsadresser.domain.Scope
import no.nav.utenlandsadresser.infrastructure.route.POSTADRESSE_MASKINPORTEN_AUTH
import no.nav.utenlandsadresser.infrastructure.route.UTENLANDSK_ID_MASKINPORTEN_AUTH
import no.nav.utenlandsadresser.plugin.configureCallLogging
import no.nav.utenlandsadresser.plugin.configureMetrics
import no.nav.utenlandsadresser.plugin.configureSerialization
import no.nav.utenlandsadresser.plugin.maskinporten.configureMaskinportenAuthentication
import no.nav.utenlandsadresser.plugin.maskinporten.validateOrganisasjonsnummer
import java.net.URI

/**
 * Setter opp Ktor-plugins som brukes av applikasjonen.
 */
context(config: UtenlandsadresserConfig)
fun Application.setupApplicationPlugins(): Plugins {
    val meterRegistry = configureMetrics()
    configureSerialization()
    configureCallLogging()
    val issuer = Issuer(config.maskinporten.issuer)
    val jwkProvider = JwkProviderBuilder(URI.create(config.maskinporten.jwksUri).toURL()).build()
    configureMaskinportenAuthentication(
        configurationName = POSTADRESSE_MASKINPORTEN_AUTH,
        issuer = issuer,
        requiredScopes = setOf(Scope(config.maskinporten.postadresseScope)),
        jwkProvider = jwkProvider,
        jwtValidationBlock = validateOrganisasjonsnummer(config.maskinporten.consumers),
    )
    configureMaskinportenAuthentication(
        configurationName = UTENLANDSK_ID_MASKINPORTEN_AUTH,
        issuer = issuer,
        requiredScopes = setOf(Scope(config.maskinporten.utenlandskIdScope)),
        jwkProvider = jwkProvider,
        jwtValidationBlock = validateOrganisasjonsnummer(config.maskinporten.consumers),
    )

    return Plugins(
        meterRegistry = meterRegistry,
    )
}
