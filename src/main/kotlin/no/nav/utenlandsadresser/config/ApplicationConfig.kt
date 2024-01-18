package no.nav.utenlandsadresser.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.utenlandsadresser.KtorEnv


fun getApplicationConfig(ktorEnv: KtorEnv): Config = when (ktorEnv) {
    KtorEnv.LOCAL -> "application.conf"
    KtorEnv.DEV_GCP -> "application-dev-gcp.conf"
    KtorEnv.PROD_GCP -> "application-prod-gcp.conf"
}.let { ConfigFactory.load(it) }
